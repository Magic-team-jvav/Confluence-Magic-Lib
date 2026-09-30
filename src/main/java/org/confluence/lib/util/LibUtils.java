package org.confluence.lib.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.QuartPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.phys.Vec2;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.component.NbtComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
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

    public static <T> void resetDataComponent(ItemStack itemStack, DataComponentType<T> type) {
        T value = itemStack.getPrototype().get(type);
        if (value == null) {
            itemStack.remove(type);
        } else {
            itemStack.set(type, value);
        }
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

    /// 可于游戏加载早期阶段判断
    ///
    /// 不能在mixin plugin中使用
    public static boolean isModLoaded(String modid) {
        return LoadingModList.get().getModFileById(modid) != null;
    }
}
