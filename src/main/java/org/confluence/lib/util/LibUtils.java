package org.confluence.lib.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.WaterAnimal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.entity.PartEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.component.NbtComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class LibUtils {
    public static final Direction[] HORIZONTAL = new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final int MAX_STACK_SIZE = 9999;
    public static final String NO_DROPS_TAG = "confluence:no_drops";
    public static final EffectCure DENY_HEAL = EffectCure.get("confluence:deny_heal");

    @ApiStatus.Internal
    public static void forMixin$Inject() {}

    @ApiStatus.Internal
    public static <T> T forMixin$ModifyExpression(T value) {
        return value;
    }

    /**
     * 获取两个向量的角度（弧度）
     *
     * @param a 向量a
     * @param b 向量b
     * @return 两个向量的角度（弧度）
     */
    public static float getAngleRadians(Vec2 a, Vec2 b) {
        return getAngleRadians(a.x, a.y, b.x, b.y);
    }

    /**
     * 获取两个向量的角度（弧度）
     *
     * @param ax 向量a的x坐标
     * @param ay 向量a的y坐标
     * @param bx 向量b的x坐标
     * @param by 向量b的y坐标
     * @return 角度（弧度）
     */
    public static float getAngleRadians(double ax, double ay, double bx, double by) {
        return (float) (Math.atan2(by - ay, bx - ax)) + 3.141f;// + (a.x > b.x ? Math.PI : 0));
    }

    public static float rotLerp(float a, float from, float to) {
        return Mth.rotLerp(a, from, to);
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

    /// @param a 形参的方块实体类型
    /// @param b 注册的方块实体类型
    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> @Nullable BlockEntityTicker<A> getTicker(BlockEntityType<A> a, BlockEntityType<E> b, BlockEntityTicker<? super E> ticker) {
        return a == b ? (BlockEntityTicker<A>) ticker : null;
    }

    /// 为专家?在处理if...else if时应先使用:
    ///
    /// @see LibUtils#isMaster(Level, BlockPos)
    public static boolean isAtLeastExpert(Level level, BlockPos pos) {
        return level.getCurrentDifficultyAt(pos).getEffectiveDifficulty() >= 1.5F;
    }

    public static boolean isAtLeastExpert(Level level) {
        return level.getDifficulty().getId() > Difficulty.EASY.getId();
    }

    /// 为大师?在处理if...else if时应先使用此方法
    public static boolean isMaster(Level level, BlockPos pos) {
        return level.getCurrentDifficultyAt(pos).getEffectiveDifficulty() >= 2.25F;
    }

    public static boolean isMaster(Level level) {
        return level.getDifficulty().getId() > Difficulty.NORMAL.getId();
    }

    /// 根据游戏难度选择值
    ///
    /// @param classic 经典难度的值
    /// @param expert  专家难度的值
    /// @return 选择到的值
    public static <T> T switchByDifficulty(Level level, BlockPos pos, T classic, T expert) {
        return switchByDifficulty(level, pos, classic, expert, expert, expert);
    }

    /// 根据游戏难度选择值
    ///
    /// @param classic 经典难度的值
    /// @param expert  专家难度的值
    /// @param master  大师难度的值
    /// @return 选择到的值
    public static <T> T switchByDifficulty(Level level, BlockPos pos, T classic, T expert, T master) {
        return switchByDifficulty(level, pos, classic, expert, master, master);
    }

    /// 根据游戏难度选择值
    ///
    /// @param classic   经典难度的值
    /// @param expert    专家难度的值
    /// @param master    大师难度的值
    /// @param legendary 传奇难度的值
    /// @return 选择到的值
    public static <T> T switchByDifficulty(Level level, BlockPos pos, T classic, T expert, T master, T legendary) {
        float difficulty = level.getCurrentDifficultyAt(pos).getEffectiveDifficulty();
        if (difficulty >= 3) return legendary;
        if (difficulty >= 2.25F) return master;
        if (difficulty >= 1.5F) return expert;
        return classic; // 0.75F
    }

    public static int getSlotIndex(@Nullable EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            case null, default -> -1;
        };
    }

    public static int getMaxStackSize(int original) {
        return Math.max(original, MAX_STACK_SIZE);
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

    public static boolean isDev() {
        return !FMLEnvironment.production;
    }

    public static void devRun(Runnable runnable) {
        if (isDev()) {
            runnable.run();
        }
    }

    public static void setItemAndDropChance(Mob mob, DifficultyInstance difficulty, EquipmentSlot slot, Item item, float chance) {
        ItemStack itemStack = item.getDefaultInstance();
        float enchantChance = (slot.getType() == EquipmentSlot.Type.HAND ? 0.25F : 0.5F) * difficulty.getSpecialMultiplier();
        if (mob.getRandom().nextFloat() < enchantChance) {
            EnchantmentHelper.enchantItemFromProvider(itemStack, mob.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, mob.getRandom());
        }
        mob.setItemSlot(slot, itemStack);
        mob.setDropChance(slot, chance);
    }

    public static CompoundTag getItemStackNbt(ItemStack stack) {
        return getItemStackNbtNoCopy(stack).copy();
    }

    public static CompoundTag getItemStackNbtNoCopy(ItemStack stack) {
        NbtComponent nbtComponent = stack.get(ConfluenceMagicLib.NBT);
        if (nbtComponent == null) {
            CompoundTag nbt = new CompoundTag();
            stack.set(ConfluenceMagicLib.NBT, new NbtComponent(nbt));
            return nbt;
        }
        return nbtComponent.nbt();
    }

    public static @Nullable CompoundTag getItemStackNbtIfPresent(ItemStack stack) {
        NbtComponent component = stack.get(ConfluenceMagicLib.NBT);
        if (component == null) return null;
        return component.nbt();
    }

    public static void updateItemStackNbt(ItemStack stack, Consumer<CompoundTag> consumer) {
        NbtComponent nbtComponent = stack.get(ConfluenceMagicLib.NBT);
        CompoundTag nbt = nbtComponent == null ? new CompoundTag() : nbtComponent.nbt().copy();
        consumer.accept(nbt);
        stack.set(ConfluenceMagicLib.NBT, new NbtComponent(nbt));
    }

    public static String toTitleCase(String raw) {
        return Arrays.stream(raw.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /// 将绝对坐标压缩为相对坐标
    public static int compressRelativePos(BlockPos pos) {
        return ((pos.getX() & 0xF) << 16) | ((pos.getY() + 2048) << 4) | (pos.getZ() & 0xF);
    }

    /// 将相对坐标解压为绝对坐标
    public static BlockPos decompressRelativePos(ChunkPos pos, int compressed) {
        int x = (compressed >>> 16) & 0xF;
        int y = ((compressed >>> 4) & 0xFFF) - 2048;
        int z = compressed & 0xF;
        return pos.getBlockAt(x, y, z);
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

    public static boolean isPhysicalClient() {
        return FMLEnvironment.dist.isClient();
    }

    /// @return 单人模式中为false；客户端连接服务端时，客户端为true，服务端为false
    /// @apiNote 你应该在逻辑服务端启动后调用这个方法，且仅适用于在逻辑服务端调用
    public static boolean isLogicalClient() {
        return isPhysicalClient() && ServerLifecycleHooks.getCurrentServer() == null;
    }

    public static boolean isPhysicalServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }

    /// @return 逻辑客户端为false, 逻辑服务端为true
    /// @apiNote 你应该在逻辑服务端启动后调用这个方法
    public static boolean isLogicalServer() {
        if (isPhysicalServer()) return true;
        return ServerLifecycleHooks.getCurrentServer() != null && ServerLifecycleHooks.getCurrentServer().isSameThread();
    }

    @Deprecated(since = "1.3.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
    public static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        return LibMathUtils.cubicBezier(t, p0, p1, p2, p3);
    }

    public static <T> void resetDataComponent(ItemStack itemStack, DataComponentType<T> type) {
        T value = itemStack.getPrototype().get(type);
        if (value == null) {
            itemStack.remove(type);
        } else {
            itemStack.set(type, value);
        }
    }

    @Deprecated(since = "1.3.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
    public static boolean checkChance(float value, RandomSource random) {
        return LibMathUtils.checkChance(value, random);
    }

    @Deprecated(since = "1.3.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
    public static boolean checkChance(double value, RandomSource random) {
        return LibMathUtils.checkChance(value, random);
    }

    public static <K, V> Map<K, V> convertTupleListToMap(List<Tuple<K, V>> list) {
        ImmutableMap.Builder<K, V> map = ImmutableMap.builder();
        for (Tuple<K, V> tuple : list) {
            map.put(tuple.getA(), tuple.getB());
        }
        return map.build();
    }

    public static <K, V> List<Tuple<K, V>> convertMapToTupleList(Map<K, V> map) {
        ImmutableList.Builder<Tuple<K, V>> list = ImmutableList.builder();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            list.add(new Tuple<>(entry.getKey(), entry.getValue()));
        }
        return list.build();
    }

    public static boolean isAnimal(LivingEntity living) {
        return !(living instanceof Enemy) && (living instanceof Animal || living instanceof WaterAnimal);
    }

    public static ResourceLocation withUniqueSuffix(ResourceLocation id) {
        UUID uuid = UUID.randomUUID();
        return id.withSuffix("_" + uuid.toString().replace("-", ""));
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
    /// @see LibUtils#tryFindBeImpacted(Entity) 适合查询直接受影响的实体的方法
    @Contract("null -> null; !null -> !null")
    public static @Nullable Entity getOwner(@Nullable Entity entity) {
        Entity owner = switch (entity) {
            case PartEntity<?> partEntity -> partEntity.getParent();
            case OwnableEntity ownableEntity -> ownableEntity.getOwner();
            case TraceableEntity traceableEntity -> traceableEntity.getOwner();
            case null, default -> entity;
        };
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

    public static @Nullable ChunkAccess getChunkIfLoaded(ServerChunkCache chunkSource, BlockPos blockPos) {
        return getChunkIfLoaded(chunkSource, SectionPos.blockToSectionCoord(blockPos.getX()), SectionPos.blockToSectionCoord(blockPos.getZ()));
    }

    public static @Nullable ChunkAccess getChunkIfLoaded(ServerLevel level, BlockPos blockPos) {
        return getChunkIfLoaded(level.getChunkSource(), blockPos);
    }

    public static @Nullable ChunkAccess getChunkIfLoaded(ServerChunkCache chunkSource, ChunkPos chunkPos) {
        return getChunkIfLoaded(chunkSource, chunkPos.x, chunkPos.z);
    }

    public static @Nullable ChunkAccess getChunkIfLoaded(ServerLevel level, ChunkPos chunkPos) {
        return getChunkIfLoaded(level.getChunkSource(), chunkPos);
    }

    /// 较大程度地减小开销，切记要在服务器线程调用！
    public static @Nullable ChunkAccess getChunkIfLoaded(ServerChunkCache chunkSource, int cx, int cz) {
        CompletableFuture<ChunkResult<ChunkAccess>> future = chunkSource.getChunkFutureMainThread(cx, cz, ChunkStatus.FULL, false);
        if (future != GenerationChunkHolder.UNLOADED_CHUNK_FUTURE && future.isDone()) {
            return future.join().orElse(null);
        }
        return null;
    }

    public static @Nullable ChunkAccess getChunkIfLoaded(ServerLevel level, int cx, int cz) {
        return getChunkIfLoaded(level.getChunkSource(), cx, cz);
    }

    /// @return 一个BiomeManager，通过它获得的群系，如果是The Void，则表示该坐标所在区块未加载
    /// @apiNote 你应该在服务器线程调用这个方法
    public static BiomeManager getBiomeManagerThatChunkMustBeLoaded(ServerLevel level) {
        return level.getBiomeManager().withDifferentSource((qx, qy, qz) -> {
            ChunkAccess access = LibUtils.getChunkIfLoaded(level, QuartPos.toSection(qx), QuartPos.toSection(qz));
            if (access == null) return level.registryAccess().holderOrThrow(Biomes.THE_VOID);
            return access.getNoiseBiome(qx, qy, qz);
        });
    }

    @Deprecated(since = "1.3.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
    public static int multiplyInt(int original, float factor, RandomSource random) {
        return LibMathUtils.multiplyInt(original, factor, random);
    }

    @Deprecated(since = "1.3.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
    public static int divideInt(int original, float factor, RandomSource random) {
        return LibMathUtils.divideInt(original, factor, random);
    }

    public static boolean canHitEntity(@Nullable Entity target, @Nullable Entity owner) {
        if (target == null || target.isRemoved()) return false; // 有模组把target写成了null
        target = getOwner(target);
        if (owner == target || !target.isAttackable() || !target.canBeHitByProjectile() || target instanceof ArmorStand) {
            return false;
        }
        return owner == null || (!owner.isPassengerOfSameVehicle(target)/* && !target.skipAttackInteraction(owner)*/);
    }

    public static boolean isSingleplayerOwner(ServerPlayer player) {
        return player.server.isSingleplayerOwner(player.getGameProfile());
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
            HitResult hitResult = subHits.getFirst();
            if (hitResult instanceof EntityHitResult entityHitResult &&
                    entityHitResult.getEntity() instanceof LivingEntity livingEntity) {
                return livingEntity;
            }
        }
        return null;
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

    /// 把 A 对 B 的击退动量结算成实际位移：先按被击退者的 `KNOCKBACK_RESISTANCE` 折减，
    /// 再按攻击者的 `ATTACK_KNOCKBACK` 放大，方向取 A→B 单位向量，另加 `motionY` 的纵向分量。
    ///
    /// 1.20 侧在 `org.confluence.lib.util.LibEntityUtils:166`（别名表把 `LibEntityUtils` 等价到 1.21 的
    /// `LibUtils`）；1.21 侧此前缺这一支，枪械 G3′ 的 `BaseBulletEntity:393` 要用，故按 1.20 逐字回补
    /// （依赖同批补的 `LibMathUtils.getVectorA2B`）。
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

    /// 可于游戏加载早期阶段判断
    ///
    /// 不能在mixin plugin中使用
    public static boolean isModLoaded(String modid) {
        return LoadingModList.get().getModFileById(modid) != null;
    }

    public static void poweringCreeper(Creeper creeper) {
        creeper.getEntityData().set(Creeper.DATA_IS_POWERED, true);
    }
}
