package org.confluence.lib.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class LibUtils {
    public static final Direction[] HORIZONTAL = new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final int MAX_STACK_SIZE = 9999;
    public static final Codec<BlockPos> BLOCK_POS_CODEC = Codec.STRING.xmap(str -> BlockPos.of(Long.parseLong(str)), pos -> Long.toString(pos.asLong()));
    public static final String NO_DROPS_TAG = "confluence:no_drops";

    public static void createItemEntity(ItemStack itemStack, double x, double y, double z, Level level, int pickUpDelay) {
        if (itemStack.isEmpty()) return;
        ItemEntity itemEntity = new ItemEntity(level, x, y, z, itemStack);
        itemEntity.setPickUpDelay(pickUpDelay);
        level.addFreshEntity(itemEntity);
    }

    public static void createItemEntity(ItemStack itemStack, Vec3 pos, Level level, int pickUpDelay) {
        createItemEntity(itemStack, pos.x, pos.y, pos.z, level, pickUpDelay);
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

    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> getTicker(BlockEntityType<A> a, BlockEntityType<E> b, BlockEntityTicker<? super E> ticker) {
        return a == b ? (BlockEntityTicker<A>) ticker : null;
    }

    /**
     * 为专家?在处理if...else if时应先使用:
     *
     * @see LibUtils#isMaster(Level, BlockPos)
     */
    public static boolean isAtLeastExpert(Level level, BlockPos pos) {
        return level.getCurrentDifficultyAt(pos).getEffectiveDifficulty() >= 1.5F;
    }

    /**
     * 为大师?在处理if...else if时应先使用此方法
     */
    public static boolean isMaster(Level level, BlockPos pos) {
        return level.getCurrentDifficultyAt(pos).getEffectiveDifficulty() >= 2.25F;
    }

    /**
     * 根据游戏难度选择值
     *
     * @param classic 经典难度的值
     * @param expert  专家难度的值
     * @param master  大师难度的值
     * @return 选择到的值
     */
    public static <T> T switchByDifficulty(Level level, BlockPos blockPos, T classic, T expert, T master) {
        float difficulty = level.getCurrentDifficultyAt(blockPos).getEffectiveDifficulty();
        if (difficulty >= 2.25F) return master;
        if (difficulty >= 1.5F) return expert;
        return classic; // 0.75F
    }

    /**
     * 根据游戏难度选择值
     *
     * @param classic   经典难度的值
     * @param expert    专家难度的值
     * @param master    大师难度的值
     * @param legendary 传奇难度的值
     * @return 选择到的值
     */
    public static <T> T switchByDifficulty(Level level, BlockPos blockPos, T classic, T expert, T master, T legendary) {
        float difficulty = level.getCurrentDifficultyAt(blockPos).getEffectiveDifficulty();
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

    public static void devRun(Runnable runnable) {
        if (!FMLEnvironment.production) {
            runnable.run();
        }
    }

    public static <A, B> Codec<Tuple<A, B>> tupleCodec(Codec<A> aCodec, Codec<B> bCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                aCodec.fieldOf("a").forGetter(Tuple::getA),
                bCodec.fieldOf("b").forGetter(Tuple::getB)
        ).apply(instance, Tuple::new));
    }

    public static <L, M, R> Codec<ImmutableTriple<L, M, R>> tripleCodec(Codec<L> lCodec, Codec<M> mCodec, Codec<R> rCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                lCodec.fieldOf("l").forGetter(ImmutableTriple::getLeft),
                mCodec.fieldOf("m").forGetter(ImmutableTriple::getMiddle),
                rCodec.fieldOf("r").forGetter(ImmutableTriple::getRight)
        ).apply(instance, ImmutableTriple::new));
    }
}
