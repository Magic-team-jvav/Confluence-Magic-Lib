package org.confluence.lib.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Tuple;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.api.event.EffectSwitchableCheckEvent;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.common.component.NbtComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.component.PortDataComponentType;
import org.mesdag.portlib.diff.Diff;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.registries.PortRegistryEntry;
import org.mesdag.portlib.wrapper.PortEnvironment;
import org.mesdag.portlib.wrapper.common.PortEffectCure;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class LibUtils {
    public static final Direction[] HORIZONTAL = new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final int MAX_STACK_SIZE = 9999;
    public static final String NO_DROPS_TAG = "confluence:no_drops";
    public static final PortEffectCure DENY_HEAL = PortEffectCure.get("confluence:deny_heal");

    @ApiStatus.Internal
    public static void forMixin$Inject() {}

    @ApiStatus.Internal
    public static <T> T forMixin$ModifyExpression(T value) {
        return value;
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
        if (slot == null) return -1;
        return switch (slot) {
            case HEAD -> 0;
            case CHEST -> 1;
            case LEGS -> 2;
            case FEET -> 3;
            default -> -1;
        };
    }

    // todo Container, ItemStack
    public static int getMaxStackSize(int original) {
        return Math.max(original, MAX_STACK_SIZE);
    }

    public static boolean isDev() {
        return PortEnvironment.isDeveloper();
    }

    public static void devRun(Runnable runnable) {
        if (isDev()) {
            runnable.run();
        }
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

    public static boolean isPhysicalClient() {
        return PortEnvironment.isPhysicalClient();
    }

    /// @return 单人模式中为false；客户端连接服务端时，客户端为true，服务端为false
    /// @apiNote 你应该在逻辑服务端启动后调用这个方法，且仅适用于在逻辑服务端调用
    public static boolean isLogicalClient() {
        return PortEnvironment.isLogicalClient();
    }

    public static boolean isPhysicalServer() {
        return PortEnvironment.isPhysicalServer();
    }

    /// @return 逻辑客户端为false, 逻辑服务端为true
    /// @apiNote 你应该在逻辑服务端启动后调用这个方法
    public static boolean isLogicalServer() {
        return PortEnvironment.isLogicalServer();
    }

    public static <T> void resetDataComponent(ItemStack stack, PortDataComponentType<T> type) {
        T value = stack.getPrototype().get(type);
        if (value == null) {
            stack.remove(type);
        } else {
            stack.set(type, value);
        }
    }

    @Diff
    public static <T> void resetDataComponent(ItemStack stack, PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<T>> type) {
        resetDataComponent(stack, type.get());
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

    public static ResourceLocation withUniqueSuffix(ResourceLocation id) {
        UUID uuid = UUID.randomUUID();
        return id.withSuffix("_" + uuid.toString().replace("-", ""));
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
        CompletableFuture<Either<ChunkAccess, ChunkHolder.ChunkLoadingFailure>> future = chunkSource.getChunkFutureMainThread(cx, cz, ChunkStatus.FULL, false);
        if (future != ChunkHolder.UNLOADED_CHUNK_FUTURE && future.isDone()) {
            return future.join().map(Function.identity(), failure -> null);
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
            if (access == null) {
                return level.registryAccess().holderOrThrow(Biomes.THE_VOID);
            }
            return access.getNoiseBiome(qx, qy, qz);
        });
    }

    /// 可于游戏加载早期阶段判断
    ///
    /// 不能在mixin plugin中使用
    public static boolean isModLoaded(String modid) {
        return PortEnvironment.isModLoaded(modid);
    }

    public static DamageSource damageSource(Level level, ResourceKey<DamageType> key, @Nullable Entity causing, @Nullable Entity direct) {
        return level.damageSources().source(key, direct, causing);
    }

    public static DamageSource damageSource(Level level, ResourceKey<DamageType> key, @Nullable Entity entity) {
        return level.damageSources().source(key, entity, entity);
    }

    public static DamageSource damageSource(Level level, ResourceKey<DamageType> key) {
        return level.damageSources().source(key, null, null);
    }

    public static int listRandom(BooleanStorage4 list, RandomSource random) {
        for (int i = 0; i < 100; i++) {
            int listW = random.nextInt(list.size());
            if (!list.get(listW)) {
                return listW;
            }
        }
        return 0;
    }

    public static boolean isSwitchableEffect(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect();
        boolean switchable = effect == LibEffects.GRAVITATION.get() ? instance.getAmplifier() <= 0 : effect.isBeneficial();
        return PortEventHandler.postEventWithReturn(new EffectSwitchableCheckEvent(instance, switchable)).isSwitchable();
    }
}
