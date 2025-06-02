package org.confluence.lib.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Tuple;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.providers.VanillaEnchantmentProviders;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.EffectCure;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.component.NbtComponent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class LibUtils {
    public static final Direction[] HORIZONTAL = new Direction[]{Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.NORTH};
    public static final Direction[] DIRECTIONS = Direction.values();
    public static final int MAX_STACK_SIZE = 9999;
    public static final String NO_DROPS_TAG = "confluence:no_drops";
    public static final EffectCure DENY_HEAL = EffectCure.get("confluence:deny_heal");
    public static final Codec<Vec2> VEC_2_CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(vec2 -> vec2.x),
            Codec.FLOAT.fieldOf("y").forGetter(vec2 -> vec2.y)
    ).apply(instance, Vec2::new));
    public static final StreamCodec<ByteBuf, Vec2> VEC_2_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, vec2 -> vec2.x,
            ByteBufCodecs.FLOAT, vec2 -> vec2.y,
            Vec2::new
    );

    @ApiStatus.Internal
    public static void forMixin$Inject() {}

    @ApiStatus.Internal
    public static <T> T forMixin$ModifyExpression(T value) {
        return value;
    }

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

    /**
     * @param a 形参的方块实体类型
     * @param b 注册的方块实体类型
     */
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

    public static void setItemAndDropChance(Mob mob, DifficultyInstance difficulty, EquipmentSlot slot, Item item, float chance) {
        ItemStack itemStack = item.getDefaultInstance();
        float enchantChance = (slot.getType() == EquipmentSlot.Type.HAND ? 0.25F : 0.5F) * difficulty.getSpecialMultiplier();
        if (mob.getRandom().nextFloat() < enchantChance) {
            EnchantmentHelper.enchantItemFromProvider(itemStack, mob.registryAccess(), VanillaEnchantmentProviders.MOB_SPAWN_EQUIPMENT, difficulty, mob.getRandom());
        }
        mob.setItemSlot(slot, itemStack);
        mob.setDropChance(slot, chance);
    }

    public static CompoundTag getItemStackNbt(ItemStack itemStack) {
        NbtComponent nbtComponent = itemStack.get(ConfluenceMagicLib.NBT);
        if (nbtComponent == null) {
            CompoundTag nbt = new CompoundTag();
            itemStack.set(ConfluenceMagicLib.NBT, new NbtComponent(nbt));
            return nbt;
        }
        return nbtComponent.nbt().copy();
    }

    public static @Nullable CompoundTag getItemStackNbtIfPresent(ItemStack itemStack) {
        NbtComponent component = itemStack.get(ConfluenceMagicLib.NBT);
        if (component == null) return null;
        return component.nbt();
    }

    public static void updateItemStackNbt(ItemStack itemStack, Consumer<CompoundTag> consumer) {
        NbtComponent nbtComponent = itemStack.get(ConfluenceMagicLib.NBT);
        CompoundTag nbt;
        if (nbtComponent == null) {
            nbt = new CompoundTag();
        } else {
            nbt = nbtComponent.nbt().copy();
        }
        consumer.accept(nbt);
        itemStack.set(ConfluenceMagicLib.NBT, new NbtComponent(nbt));
    }

    public static String toTitleCase(String raw) {
        return Arrays.stream(raw.split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * 将绝对坐标压缩为相对坐标
     */
    public static int compressRelativePos(BlockPos pos) {
        return ((pos.getX() & 0xF) << 16) | ((pos.getY() + 2048) << 4) | (pos.getZ() & 0xF);
    }

    /**
     * 将相对坐标解压为绝对坐标
     */
    public static BlockPos decompressRelativePos(ChunkPos chunkPos, int compressed) {
        int x = (compressed >>> 16) & 0xF;
        int y = ((compressed >>> 4) & 0xFFF) - 2048;
        int z = compressed & 0xF;
        return chunkPos.getBlockAt(x, y, z);
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

    /**
     * @return 单人模式中为false；客户端连接服务端时，客户端为true，服务端为false
     * @apiNote 你应该在逻辑服务端启动后调用这个方法
     */
    public static boolean isLogicalAndPhysicalClient() {
        return FMLEnvironment.dist.isClient() && ServerLifecycleHooks.getCurrentServer() == null;
    }
}
