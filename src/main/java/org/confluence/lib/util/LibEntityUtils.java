package org.confluence.lib.util;

import PortLib.extensions.java.util.List.PortListExtension;
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
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class LibEntityUtils {
    public static boolean canHitEntity(@Nullable Entity target, @Nullable Entity owner) {
        if (target == null || target.isRemoved()) return false;
        target = getOwner(target);
        if (owner == target || !target.isAttackable() || !target.canBeHitByProjectile() || target instanceof ArmorStand) {
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
        // 扩大包围盒
        AABB aabb;
        if (owner == null) {
            aabb = new AABB(ori, end).inflate(range);
        } else {
            aabb = owner.getBoundingBox().inflate(range);
        }
        Vec3 direction = end.subtract(ori);
        List<HitResult> hits = new ArrayList<>();
        List<HitResult> subHits = new ArrayList<>();
        List<? extends Entity> entities = level.getEntities(owner, aabb, entity1 -> entity1.isPickable() && entity1.isAlive() && filter.test(entity1));
        for (var e : entities) {
            // 获取视线交点
            Vec3 vec3 = e.getBoundingBox().clip(ori, end).orElse(null);
            // 优先指向的目标
            if (vec3 != null) {
                //System.multiOut.println("point directly");
                EntityHitResult entityHitResult = new EntityHitResult(e, vec3);
                hits.add(entityHitResult);
            } else if (hits.isEmpty() && LibMathUtils.angleBetween(e.position().subtract(ori), direction) < Math.toRadians(maxAngle)) {
                // 自瞄其他夹角小于一定度数的目标
                EntityHitResult entityHitResult = new EntityHitResult(e, e.position());
                subHits.add(entityHitResult);
            }
        }

        if (!hits.isEmpty()) {
            // 射线命中的目标 按距离排序
            hits.sort((o1, o2) -> {
                double v1 = o1.getLocation().distanceToSqr(ori);
                double v2 = o2.getLocation().distanceToSqr(ori);
                if (v1 == v2) return 0;
                return v1 < v2 ? -1 : 1;
            });
            for (HitResult hitResult : hits) {
                // 这里改了一下，使用这个方法的武器可能会出问题。原本是只锁定怪物，如果武器出现了攻击到不该攻击的目标的话，在filter里面排除一下
                if (hitResult instanceof EntityHitResult entityHitResult && (entityHitResult.getEntity() instanceof LivingEntity living)) {
                    return living;
                }
            }
        } else if (!subHits.isEmpty()) {
            // 未命中的目标 按角度排序
            subHits.sort((o1, o2) -> {
                double v1 = LibMathUtils.angleBetween(o1.getLocation().subtract(ori), direction);
                double v2 = LibMathUtils.angleBetween(o2.getLocation().subtract(ori), direction);
                if (v1 == v2) return 0;
                return v1 < v2 ? -1 : 1;
            });
            HitResult hitResult = PortListExtension.getFirst(subHits);
            if (hitResult instanceof EntityHitResult entityHitResult &&
                    entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
    }

    /// 获取包围盒内锥形射线内的所有目标
    ///
    /// @param ori      起始点
    /// @param end      终止点
    /// @param range    若owner为null，则为包围盒范围，否则无效
    /// @param maxAngle 最大角度
    /// @return 若直接命中，返回命中的目标；否则返回最近有效的目标
    public static List<LivingEntity> getAABBAngleTargets(Vec3 ori, Vec3 end, Level level, @Nullable Entity owner, double range, double maxAngle, Predicate<Entity> filter) {
        // 扩大包围盒
        AABB aabb;
        if (owner != null) {
            aabb = owner.getBoundingBox().inflate(range);
        } else {
            aabb = new AABB(ori, end).inflate(range);
        }
        Vec3 direction = end.subtract(ori);
        List<HitResult> hits = new ArrayList<>();
        List<? extends Entity> entities = level.getEntities(owner, aabb, entity1 -> entity1.isPickable() && entity1.isAlive() && filter.test(entity1));
        for (var e : entities) {
            // 获取视线交点
            Vec3 vec3 = e.getBoundingBox().clip(ori, end).orElse(null);
            // 优先指向的目标
            if (vec3 != null) {
                EntityHitResult entityHitResult = new EntityHitResult(e, vec3);
                hits.add(entityHitResult);
            } else if (hits.isEmpty() && LibMathUtils.angleBetween(e.position().subtract(ori), direction) < Math.toRadians(maxAngle)) {
                // 自瞄其他夹角小于一定度数的目标
                EntityHitResult entityHitResult = new EntityHitResult(e, e.position());
                hits.add(entityHitResult);
            }
        }

        if (!hits.isEmpty()) {
            List<LivingEntity> livingEntities = new ArrayList<>();
            for (HitResult hitResult : hits) {
                if (hitResult instanceof EntityHitResult entityHitResult && (entityHitResult.getEntity() instanceof LivingEntity living)) {
                    livingEntities.add(living);
                }
            }
            return livingEntities;
        }
        return List.of();
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
        if (entity instanceof PartEntity<?> partEntity) {
            return partEntity.getParent();
        } else if (entity instanceof OwnableEntity ownableEntity) {
            return ownableEntity.getOwner();
        } else if (entity instanceof TraceableEntity traceableEntity) {
            return traceableEntity.getOwner();
        }
        return entity;
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
//        if (mob.getRandom().nextFloat() < enchantChance) {
//            EnchantmentHelper.enchantItemFromProvider(itemStack, mob.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, mob.getRandom());
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

    /// 更新实体朝向
    public static void updateEntityRotation(Entity entity, Vec3 dir) {
        float[] angle = LibMathUtils.dirToRot(dir, true);
        entity.setYRot(angle[0]);
        entity.setXRot(angle[1]);
    }
}
