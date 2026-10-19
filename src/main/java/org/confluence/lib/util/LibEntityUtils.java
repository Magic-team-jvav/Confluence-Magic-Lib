package org.confluence.lib.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.confluence.lib.common.entity.PartHitTarget;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class LibEntityUtils {
    public static boolean canHitEntity(@Nullable Entity target, @Nullable Entity owner) {
        if (target == null || target.isRemoved()) return false;
        boolean part = target instanceof PartEntity<?> || target instanceof PartHitTarget;
        if (part && (!target.canBeHitByProjectile()
                || target instanceof PartHitTarget hitTarget && !hitTarget.acceptsDirectHit())) return false;
        target = part ? encounterOwner(target) : getOwner(target);
        if (owner == target || !target.isAlive() || target.isRemoved() || !target.isAttackable()
                || (!part && !target.canBeHitByProjectile()) || target instanceof ArmorStand) {
            return false;
        }
        return owner == null || (!owner.isPassengerOfSameVehicle(target));
    }

    public static void poweringCreeper(Creeper creeper) {
        creeper.getEntityData().set(Creeper.DATA_IS_POWERED, true);
    }

    /// 获取包围盒内锥形射线内的目标
    ///
    /// @param ori      起始点
    /// @param end      终止点
    /// @param range    若owner为null，则为包围盒范围，否则无效
    /// @param maxAngle 最大角度
    /// @return 若直接命中，返回命中的目标；否则返回最近有效的目标
    public static @Nullable LivingEntity getAABBAngleTarget(Vec3 ori, Vec3 end, Level level, @Nullable Entity owner, double range, double maxAngle, Predicate<Entity> filter) {
        return livingTarget(getAABBAngleHitTarget(ori, end, level, owner, range, maxAngle, filter));
    }

    /// 与生命实体接口共用优先级，但保留实际受击部位供追踪武器瞄准。
    public static @Nullable Entity getAABBAngleHitTarget(Vec3 ori, Vec3 end, Level level, @Nullable Entity owner,
            double range, double maxAngle, Predicate<Entity> filter) {
        List<EntityHitResult> hits = angleHits(ori, end, level, owner, range, maxAngle, filter);
        return hits.isEmpty() ? null : hits.get(0).getEntity();
    }

    public static List<LivingEntity> getAABBAngleTargets(Vec3 ori, Vec3 end, Level level, @Nullable Entity owner,
            double range, double maxAngle, Predicate<Entity> filter) {
        Set<LivingEntity> targets = new LinkedHashSet<>();
        for (EntityHitResult hit : angleHits(ori, end, level, owner, range, maxAngle, filter)) {
            LivingEntity target = livingTarget(hit.getEntity());
            if (target != null) targets.add(target);
        }
        return new ArrayList<>(targets);
    }

    private static @Nullable LivingEntity livingTarget(@Nullable Entity hit) {
        if (hit == null) return null;
        if (hit instanceof LivingEntity living) return living;
        Entity owner = encounterOwner(hit);
        return owner instanceof LivingEntity living ? living : null;
    }

    private static List<EntityHitResult> angleHits(Vec3 ori, Vec3 end, Level level, @Nullable Entity owner,
            double range, double maxAngle, Predicate<Entity> filter) {
        AABB bounds = owner == null ? new AABB(ori, end).inflate(range) : owner.getBoundingBox().inflate(range);
        Vec3 direction = end.subtract(ori);
        List<EntityHitResult> direct = new ArrayList<>();
        List<EntityHitResult> angled = new ArrayList<>();
        for (Entity candidate : level.getEntities(owner, bounds,
                entity -> entity.isPickable() && entity.isAlive() && livingTarget(entity) != null && filter.test(entity))) {
            AABB box = candidate.getBoundingBox();
            Vec3 hit = box.contains(ori) ? ori : box.clip(ori, end).orElse(null);
            if (hit != null) {
                direct.add(new EntityHitResult(candidate, hit));
            } else {
                Vec3 center = box.getCenter();
                if (LibMathUtils.angleBetween(center.subtract(ori), direction) < Math.toRadians(maxAngle)) {
                    angled.add(new EntityHitResult(candidate, center));
                }
            }
        }
        if (!direct.isEmpty()) {
            direct.sort(Comparator.comparingDouble(hit -> hit.getLocation().distanceToSqr(ori)));
            return direct;
        }
        angled.sort(Comparator.comparingDouble(hit -> LibMathUtils.angleBetween(hit.getLocation().subtract(ori), direction)));
        return angled;
    }

    /// 给予实体B一个击退动量，方向为A→B
    ///
    /// @param a       实体A
    /// @param b       实体B
    /// @param scale   击退动量的缩放
    /// @param motionY 击退的Y轴动量
    public static void knockBackA2B(Entity a, Entity b, double scale, double motionY) {
        if (b instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) scale *= (1.0 - instance.getValue());
        }
        if (scale > 0.0) {
            LivingEntity living = null;
            if (a instanceof TraceableEntity traceable && traceable.getOwner() instanceof LivingEntity living1)
                living = living1;
            else if (a instanceof LivingEntity living1) living = living1;
            if (living != null) {
                AttributeInstance instance = living.getAttribute(Attributes.ATTACK_KNOCKBACK);
                if (instance != null) scale *= (1.0 + instance.getValue());
            }
            b.addDeltaMovement(LibMathUtils.getVectorA2B(a, b).scale(scale).add(0.0, motionY, 0.0));
        }
    }

    /// 给予实体一个击退动量，方向为vector
    ///
    /// @param attacker 击退者
    /// @param victim   被击退者
    /// @param vector   向量
    public static void knockBack(LivingEntity attacker, Entity victim, Vec3 vector) {
        double scale = 1.0;
        if (victim instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) scale *= (1.0 - instance.getValue());
        }
        if (scale > 0.0) {
            LivingEntity living;
            if (attacker instanceof TraceableEntity traceable && traceable.getOwner() instanceof LivingEntity living1)
                living = living1;
            else living = attacker;
            AttributeInstance instance = living.getAttribute(Attributes.ATTACK_KNOCKBACK);
            if (instance != null) scale *= (1.0 + instance.getValue());
            victim.addDeltaMovement(vector.scale(scale));
        }
    }

    public static boolean isSingleplayerOwner(ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile());
    }

    public static @Nullable Entity getOwner(DamageSource damageSource) {
        Entity entity = damageSource.getEntity();
        if (entity == null) return null;
        return getOwner(entity);
    }

    /// 尝试寻找该实体的所有者，如果找不到则返回该实体
    ///
    /// 适合查询始作俑者
    ///
    /// @see LibEntityUtils#tryFindBeImpacted(Entity) 适合查询直接受影响的实体的方法
    @Contract("null -> null; !null -> !null")
    public static @Nullable Entity getOwner(@Nullable Entity entity) {
        Entity owner;
        if (entity instanceof PartEntity<?> partEntity) {
            owner = partEntity.getParent();
        } else if (entity instanceof OwnableEntity ownableEntity) {
            owner = ownableEntity.getOwner();
        } else if (entity instanceof TraceableEntity traceableEntity) {
            owner = traceableEntity.getOwner();
        } else {
            owner = null;
        }
        return owner == null ? entity : owner;
    }

    /// 适合查询直接受影响的实体，如攻击本体
    @Contract("null -> null; !null -> !null")
    public static @Nullable Entity tryFindBeImpacted(@Nullable Entity entity) {
        if (entity instanceof PartEntity<?> part) {
            return part.getParent();
        }
        return entity;
    }

    public static boolean isAnimal(LivingEntity living) {
        return !(living instanceof Enemy) && (living instanceof Animal || living instanceof WaterAnimal);
    }

    public static CompoundTag getOrCreatePersistedData(Player player) {
        CompoundTag data = player.getPersistentData();
        if (data.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            return data.getCompound(Player.PERSISTED_NBT_TAG);
        }
        CompoundTag tag = new CompoundTag();
        data.put(Player.PERSISTED_NBT_TAG, tag);
        return tag;
    }

    public static void setItemAndDropChance(Mob mob, DifficultyInstance difficulty, EquipmentSlot slot, Item item, float chance) {
        ItemStack itemStack = item.getDefaultInstance();
        // todo
//        float enchantChance = (slot.getType() == EquipmentSlot.Type.HAND ? 0.25F : 0.5F) * difficulty.getSpecialMultiplier();
//        if (mob.getRandom1211().nextFloat() < enchantChance) {
//            EnchantmentHelper.enchantItemFromProvider(itemStack, mob.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, mob.getRandom1211());
//        }
        mob.setItemSlot(slot, itemStack);
        mob.setDropChance(slot, chance);
    }

    public static boolean anyHandHasItem(LivingEntity living, Predicate<ItemStack> predicate) {
        return predicate.test(living.getMainHandItem()) || predicate.test(living.getOffhandItem());
    }

    public static boolean anyHandHasItem(LivingEntity living, Item item) {
        return living.getMainHandItem().is(item) || living.getOffhandItem().is(item);
    }

    public static boolean anyHandHasItem(LivingEntity living, TagKey<Item> tag) {
        return living.getMainHandItem().is(tag) || living.getOffhandItem().is(tag);
    }

    public static void createItemEntity(ItemStack stack, double x, double y, double z, Level level, int pickUpDelay) {
        if (stack.isEmpty()) return;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, stack);
        itemEntity.setPickUpDelay(pickUpDelay);
        level.addFreshEntity(itemEntity);
    }

    public static void createItemEntity(ItemStack stack, Vec3 pos, Level level, int pickUpDelay) {
        createItemEntity(stack, pos.x, pos.y, pos.z, level, pickUpDelay);
    }

    public static void createItemEntity(Item item, int count, double x, double y, double z, Level level, int pickUpDelay) {
        if (count <= 0 || item == Items.AIR) return;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, new ItemStack(item, count));
        itemEntity.setPickUpDelay(pickUpDelay);
        level.addFreshEntity(itemEntity);
    }

    public static void createItemEntity(Item item, int count, Vec3 pos, Level level, int pickUpDelay) {
        createItemEntity(item, count, pos.x, pos.y, pos.z, level, pickUpDelay);
    }

    public static Vec3 getPlayerHandPos(Player player) {
        float i = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
        float f = player.yBodyRot * Mth.DEG_TO_RAD + 1f;
        float d0 = Mth.sin(f);
        float d1 = Mth.cos(f);
        float scale = player.getScale();
        float d2 = i * 0.25F * scale;
        float d3 = 0.8F * scale;
        return new Vec3(-d1 * d2 - d0 * d3, 0, -d0 * d2 + d1 * d3);
    }

    /// 更新实体朝向，同时同步生物的身体和头部朝向
    ///
    /// @param entity    实体
    /// @param direction 目标方向，零向量不更新朝向
    public static void updateEntityRotation(Entity entity, Vec3 direction) {
        turnEntityTowards(entity, direction, 180.0F, 180.0F);
    }

    /// 以指定最大转角更新实体朝向，同时同步生物的身体和头部朝向
    ///
    /// @param entity             实体
    /// @param direction          目标方向，零向量不更新朝向，垂直方向保留水平朝向
    /// @param maximumYawChange   最大水平转角，单位为度，0表示不改变
    /// @param maximumPitchChange 最大俯仰转角，单位为度，0表示不改变
    public static void turnEntityTowards(Entity entity, Vec3 direction, float maximumYawChange, float maximumPitchChange) {
        if (direction.lengthSqr() < 1.0E-14D) return;
        double horizontal = direction.horizontalDistance();
        float yaw = entity.getYRot();
        if (horizontal > 1.0E-7D && maximumYawChange > 0.0F) {
            float targetYaw = (float) (Mth.atan2(-direction.x, direction.z) * Mth.RAD_TO_DEG);
            yaw = Mth.approachDegrees(yaw, targetYaw, maximumYawChange);
        }
        if (maximumPitchChange > 0.0F) {
            float targetPitch = (float) (Mth.atan2(-direction.y, horizontal) * Mth.RAD_TO_DEG);
            entity.setXRot(Mth.approach(entity.getXRot(), targetPitch, maximumPitchChange));
        }
        setEntityYaw(entity, yaw);
    }

    /// 设置实体水平朝向，同时同步生物的身体和头部朝向
    ///
    /// @param entity 实体
    /// @param yaw    水平朝向，单位为度
    public static void setEntityYaw(Entity entity, float yaw) {
        entity.setYRot(yaw);
        if (entity instanceof LivingEntity living) {
            living.setYBodyRot(yaw);
            living.setYHeadRot(yaw);
        }
    }

    private static Function<Player, @Nullable Object> teamGetter = Player::getTeam;

    @ApiStatus.Internal
    public static void setTeamGetter(Function<Player, @Nullable Object> getter) {
        teamGetter = getter;
    }

    public static @Nullable Object getTeam(Player player) {
        return teamGetter.apply(player);
    }

    /// 即使所有者离线，也可通过 UUID 判断同属玩家的召唤实体。
    public interface OwnedTarget {
        UUID targetingOwnerId();
    }

    /// 模组专属的附加限制；通用生命、归属、队伍及中立生物判断始终由库层负责。
    public interface TargetingExtension {
        default boolean allowsAttack(@Nullable Entity attacker, Entity target) { return true; }
        default boolean allowsPvP(Player attacker, Player target) { return true; }
    }

    private static volatile TargetingExtension targetingExtension = new TargetingExtension() {};

    /// 在公共初始化阶段安装扩展，两项规则作为一个对象同时更新。
    public static void setTargetingExtension(TargetingExtension extension) {
        targetingExtension = Objects.requireNonNull(extension, "extension");
    }

    /// 固有敌怪阵营；中立生物对具体玩家的敌意由索敌配置额外判断。
    public static boolean isEnemyFaction(@Nullable Entity entity) {
        for (int depth = 0; entity != null && depth < 16; depth++) {
            if (entity instanceof OwnedTarget) return false;
            Entity owner = entity instanceof PartHitTarget part ? part.encounterOwner() : getOwner(entity);
            if (owner == null || owner == entity)
                return entity instanceof Enemy || entity.getType().getCategory() == MobCategory.MONSTER;
            entity = owner;
        }
        return false;
    }

    /// 武器定义或实体基类持有的不可变索敌配置。不保存射手或目标，可安全地由所有实例共享。
    /// 未配置时关闭自动索敌；搜索范围、优先级、重选时机和运动由武器自行决定。
    public record WeaponTargeting(TargetPolicy policy, boolean requiresDirectHit) {
        public static final WeaponTargeting DISABLED = new WeaponTargeting();

        public WeaponTargeting {
            Objects.requireNonNull(policy, "policy");
        }

        public WeaponTargeting() {
            this(TargetPolicy.NONE);
        }

        public WeaponTargeting(TargetPolicy policy) {
            this(policy, true);
        }

        /// 获取新目标：关系、类型、生命状态，以及配置要求的初次射弹命中资格。
        public boolean canAcquire(@Nullable Entity attacker, @Nullable Entity candidate) {
            return requiresDirectHit
                    ? LibEntityUtils.canAcquire(attacker, candidate, policy)
                    : LibEntityUtils.canKeep(attacker, candidate, policy);
        }

        /// 保持已有锁定：复查关系、类型和生命状态，不套用初次搜索范围及穿透记录。
        public boolean canKeep(@Nullable Entity attacker, @Nullable Entity candidate) {
            return LibEntityUtils.canKeep(attacker, candidate, policy);
        }

        public Predicate<Entity> filter(@Nullable Entity attacker, Predicate<Entity> extra) {
            return candidate -> canAcquire(attacker, candidate) && extra.test(candidate);
        }

        public @Nullable LivingEntity angleTarget(Vec3 start, Vec3 end, Level level,
                @Nullable Entity searchEntity, @Nullable Entity attacker, double range, double angle,
                Predicate<Entity> extra) {
            return LibEntityUtils.angleTarget(start, end, level, searchEntity, attacker, range, angle, this, extra);
        }

        /// 返回实际受击部位，调用者可以直接追踪部位中心，而不是主体的寻路包围盒。
        public @Nullable Entity angleHitTarget(Vec3 start, Vec3 end, Level level,
                @Nullable Entity searchEntity, @Nullable Entity attacker, double range, double angle,
                Predicate<Entity> extra) {
            return getAABBAngleHitTarget(start, end, level, searchEntity, range, angle, filter(attacker, extra));
        }

        public List<Entity> hitTargets(Level level, @Nullable Entity searchEntity, @Nullable Entity attacker,
                AABB bounds, Predicate<Entity> extra) {
            return level.getEntities(searchEntity, bounds,
                    candidate -> encounterOwner(candidate) instanceof LivingEntity && canAcquire(attacker, candidate) && extra.test(candidate));
        }

        public @Nullable EntityHitResult viewTarget(Player player, double reach) {
            return LibEntityUtils.viewTarget(player, reach, this);
        }
    }

    public enum TargetPolicy {
        /// 不参与自动索敌，不影响直接命中。
        NONE,
        /// 只校验可攻击关系；召唤物另保留愤怒、威胁和反击记录。
        ATTACKABLE,
        /// Enemy 接口筛选，同时允许正在敌视射手玩家的中立生物。
        ENEMY_ONLY,
        /// 完整敌对阵营判定，同时允许正在敌视射手玩家的中立生物。
        ENEMY_FACTION
    }

    /// 只解析受击部位的主体，不将召唤物替换成主人作为瞄准位置。
    public static Entity encounterOwner(Entity target) {
        if (target instanceof PartHitTarget part) return part.encounterOwner();
        Entity impacted = tryFindBeImpacted(target);
        return impacted == null ? target : impacted;
    }

    /// 解析攻击者、宠物和召唤物的阵营归属；有限遍历防止异常所有权环。
    private static @Nullable Entity affiliation(@Nullable Entity entity) {
        for (int depth = 0; entity != null && depth < 16; depth++) {
            Entity next;
            if (entity instanceof OwnedTarget summon) {
                next = entity.level().getPlayerByUUID(summon.targetingOwnerId());
            } else {
                next = entity instanceof PartHitTarget part ? part.encounterOwner() : getOwner(entity);
            }
            if (next == null || next == entity) return entity;
            entity = next;
        }
        return entity;
    }

    /// 与碰撞、穿透和搜索距离无关的关系校验；命中和索敌共用此入口。
    public static boolean allowsAttack(@Nullable Entity attacker, Entity rawTarget) {
        Entity target = encounterOwner(rawTarget);
        Entity source = affiliation(attacker);
        Entity targetSide = affiliation(target);
        if (target == attacker || attacker != null && attacker.isAlliedTo(target)
                || source != null && (source == targetSide || source.isAlliedTo(target)
                || targetSide != null && source.isAlliedTo(targetSide))) return false;
        if (source instanceof OwnedTarget sourceSummon && targetSide instanceof OwnedTarget targetSummon
                && sourceSummon.targetingOwnerId().equals(targetSummon.targetingOwnerId())) return false;
        if (source != null && target instanceof OwnedTarget summon
                && source.getUUID().equals(summon.targetingOwnerId())) return false;
        if (!targetingExtension.allowsAttack(attacker, target)) return false;
        if (source instanceof Player attackingPlayer && targetSide instanceof Player targetPlayer) {
            return attackingPlayer.canHarmPlayer(targetPlayer)
                    && targetingExtension.allowsPvP(attackingPlayer, targetPlayer);
        }
        return true;
    }

    /// 保留锁定时只复查生命、关系和类型，不重新施加初始搜索范围或碰撞次数限制。
    private static boolean canKeep(@Nullable Entity attacker, @Nullable Entity candidate, TargetPolicy policy) {
        if (policy == TargetPolicy.NONE || candidate == null || candidate.isRemoved() || !candidate.isAlive()
                || attacker != null && attacker.level() != candidate.level()) return false;
        if (candidate instanceof PartHitTarget part && !part.acceptsDirectHit()) return false;
        Entity target = encounterOwner(candidate);
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) return false;
        return target.isAlive() && !target.isRemoved() && allowsAttack(attacker, candidate)
                && switch (policy) {
                    case NONE -> false;
                    case ATTACKABLE -> true;
                    case ENEMY_ONLY, ENEMY_FACTION -> isEnemyTarget(attacker, candidate, target, policy);
                };
    }

    private static boolean isEnemyTarget(@Nullable Entity attacker, Entity candidate, Entity target, TargetPolicy policy) {
        Entity source = affiliation(attacker);
        if (source instanceof Player player && target instanceof LivingEntity living) {
            // 愤怒只针对该射手；对其他玩家或生物生气不算敌视当前玩家。
            boolean hostile = isHostileToPlayer(player, living);
            // 普通猪灵使用 Brain 而非 NeutralMob；不能按记忆模块判断中立，否则会误判猪灵蛮兵等敌怪。
            if (target instanceof NeutralMob || target instanceof Piglin)
                return hostile;
            if (hostile) return true;
        }
        return policy == TargetPolicy.ENEMY_ONLY ? target instanceof Enemy : isEnemyFaction(candidate);
    }

    /// 同时兼容普通 Mob、持久愤怒和使用 Brain 的生物；不依赖玩家是否曾经受伤。
    private static boolean isHostileToPlayer(Player player, LivingEntity target) {
        if (target instanceof Targeting targeting && targeting.getTarget() == player) return true;
        if (target instanceof NeutralMob neutralMob && neutralMob.isAngryAt(player)) return true;
        Optional<UUID> angryAt = target.getBrain().getMemoryInternal(MemoryModuleType.ANGRY_AT);
        if (angryAt != null && angryAt.filter(player.getUUID()::equals).isPresent()) return true;
        Optional<LivingEntity> attackTarget = target.getBrain().getMemoryInternal(MemoryModuleType.ATTACK_TARGET);
        return attackTarget != null && attackTarget.orElse(null) == player;
    }

    private static boolean canAcquire(@Nullable Entity attacker, @Nullable Entity candidate, TargetPolicy policy) {
        return canKeep(attacker, candidate, policy) && isHittable(attacker, candidate);
    }

    /// 命中资格不要求目标为生命实体，也不包含武器自己的穿透和冷却状态。
    public static boolean canHit(@Nullable Entity attacker, Entity candidate) {
        return isHittable(attacker, candidate) && allowsAttack(attacker, candidate);
    }

    private static boolean isHittable(@Nullable Entity attacker, Entity candidate) {
        if (!acceptsDirectHit(candidate)) return false;
        Entity target = encounterOwner(candidate);
        if (candidate instanceof PartHitTarget) {
            if (target.isRemoved() || !target.isAttackable()
                    || attacker != null && attacker.isPassengerOfSameVehicle(target)) return false;
        } else if (!canHitEntity(candidate, attacker)) {
            return false;
        }
        return true;
    }

    public static boolean acceptsDirectHit(Entity candidate) {
        if (candidate.isRemoved()) return false;
        if (candidate instanceof PartHitTarget part) return part.acceptsDirectHit();
        return canHitEntity(candidate, null);
    }

    /// 保留遍历顺序及调用方比较器；同分时返回最先遇到的目标。
    public static <T extends Entity> @Nullable T nearest(Iterable<T> candidates, Predicate<T> filter, Comparator<? super T> order) {
        T best = null;
        for (T candidate : candidates) {
            if (filter.test(candidate) && (best == null || order.compare(candidate, best) < 0)) best = candidate;
        }
        return best;
    }

    /// 沿用库内的直线命中优先、角度筛选和距离排序。searchEntity 决定原来的搜索中心。
    private static @Nullable LivingEntity angleTarget(Vec3 start, Vec3 end, Level level,
            @Nullable Entity searchEntity, @Nullable Entity attacker, double range, double angle,
            WeaponTargeting targeting, Predicate<Entity> extra) {
        return getAABBAngleTarget(start, end, level, searchEntity, range, angle, targeting.filter(attacker, extra));
    }

    /// 玩家视线锁定，保留方块遮挡、射线最近命中和原有包围盒扩张。
    private static @Nullable EntityHitResult viewTarget(Player player, double reach, WeaponTargeting targeting) {
        double squared = reach * reach;
        Vec3 from = player.getEyePosition(1.0F);
        HitResult blockHit = player.pick(reach, 1.0F, false);
        double blockDistance = blockHit.getLocation().distanceToSqr(from);
        if (blockHit.getType() != HitResult.Type.MISS) {
            squared = blockDistance;
            reach = Math.sqrt(blockDistance);
        }
        Vec3 view = player.getViewVector(1.0F);
        Vec3 to = from.add(view.x * reach, view.y * reach, view.z * reach);
        AABB box = player.getBoundingBox().expandTowards(view.scale(reach)).inflate(1.0, 1.0, 1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, from, to, box,
                targeting.filter(player, entity -> !entity.isSpectator() && entity.isPickable()), squared);
        return hit != null && hit.getLocation().distanceToSqr(from) < blockDistance ? hit : null;
    }

    /// 从弹幕位置到受击部位中心的方块视线检测；无碰撞目标沿用悠悠球原来的穿墙例外。
    public static boolean hasProjectileLineOfSight(Entity source, Entity target) {
        return target.noPhysics || source.level().clip(new ClipContext(source.position(), target.getBoundingBox().getCenter(),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, source)).getType() == HitResult.Type.MISS;
    }

    public static double horizontalVerticalDistance(Vec3 origin, Entity target) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
        return Math.sqrt(offset.x * offset.x + offset.z * offset.z) + Math.abs(offset.y);
    }

    /// Forge 部件不在普通实体 ID 索引内，客户端的追踪同步也必须查询部件集合。
    public static @Nullable Entity getEntityOrPart(Level level, int id) {
        if (id < 0) return null;
        Entity entity = level.getEntity(id);
        if (entity != null) return entity;
        for (PartEntity<?> part : level.getPartEntities()) {
            if (part.getId() == id && !part.getParent().isRemoved()) return part;
        }
        return null;
    }

    public static double manhattanDistance(Vec3 origin, Entity target) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
        return Math.abs(offset.x) + Math.abs(offset.y) + Math.abs(offset.z);
    }

    /// 以飞行方向为轴的前方矩形，计入目标半宽和半高，保留近乎竖直飞行时的备用侧轴。
    public static boolean insideForwardRectangle(Vec3 origin, Vec3 direction, Entity target,
            double forwardRange, double halfWidth, double halfHeight) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
        double forward = offset.dot(direction);
        AABB bounds = target.getBoundingBox();
        double targetWidth = Math.max(bounds.getXsize(), bounds.getZsize()) * 0.5D;
        double targetHeight = bounds.getYsize() * 0.5D;
        if (forward < -targetWidth || forward > forwardRange + targetWidth) return false;
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() <= 1.0E-10D) side = direction.cross(new Vec3(1.0D, 0.0D, 0.0D));
        side = side.normalize();
        Vec3 vertical = side.cross(direction).normalize();
        return Math.abs(offset.dot(side)) <= halfWidth + targetWidth
                && Math.abs(offset.dot(vertical)) <= halfHeight + targetHeight;
    }

    /// 召唤物保留原有中立怪愤怒判断、攻击主人判断和反击记录；记录与缓存由调用方持有。
    public static boolean isSummonThreat(Player owner, LivingEntity target, Predicate<LivingEntity> hurtHistory) {
        if (target instanceof Enemy) enemy: {
            if (target instanceof NeutralMob neutralMob) {
                if (neutralMob.isAngryAt(owner)) return true;
                break enemy;
            }
            Optional<UUID> angryAt = target.getBrain().getMemoryInternal(MemoryModuleType.ANGRY_AT);
            if (angryAt != null) {
                if (angryAt.isPresent() && angryAt.get().equals(owner.getUUID())) return true;
                break enemy;
            }
            return true;
        }
        return isHostileToPlayer(owner, target) || hurtHistory.test(target);
    }
}
