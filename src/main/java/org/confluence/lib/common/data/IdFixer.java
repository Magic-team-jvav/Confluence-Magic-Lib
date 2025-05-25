package org.confluence.lib.common.data;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.apache.commons.lang3.mutable.MutableObject;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.util.LibUtils;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Deprecated(since = "1.1.3")
@ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
public class IdFixer {
    public static final Codec<List<Tuple<Integer, IntArrayList>>> FIXED_BLOCK_MAP_CODEC = Codec.lazyInitialized(() -> {
        Codec<List<Tuple<Integer, LongArrayList>>> codec = LibUtils.tupleCodec(Codec.INT, Codec.LONG.listOf().xmap(LongArrayList::new, Function.identity())).listOf();
        return new Codec<>() {
            @Override
            public <T> DataResult<Pair<List<Tuple<Integer, IntArrayList>>, T>> decode(DynamicOps<T> ops, T input) {
                Optional<List<Tuple<Integer, LongArrayList>>> result = codec.parse(ops, input).result();
                if (result.isPresent()) {
                    List<Tuple<Integer, LongArrayList>> list = result.get();
                    List<Tuple<Integer, IntArrayList>> ret = new ArrayList<>(5 + list.size() + (list.size() / 10));
                    for (Tuple<Integer, LongArrayList> tuple : list) {
                        LongArrayList longs = tuple.getB();
                        IntArrayList integers = new IntArrayList(5 + longs.size() + (longs.size() / 10));
                        for (Long l : longs) {
                            int x = BlockPos.getX(l);
                            int y = BlockPos.getY(l);
                            int z = BlockPos.getZ(l);
                            integers.add(((x & 0xF) << 16) | ((y + 2048) << 4) | (z & 0xF));
                        }
                        ret.add(new Tuple<>(tuple.getA(), integers));
                    }
                    return DataResult.success(new Pair<>(ret, input), Lifecycle.stable());
                }
                return DataResult.error(() -> "Error decoding!");
            }

            @Override
            public <T> DataResult<T> encode(List<Tuple<Integer, IntArrayList>> input, DynamicOps<T> ops, T prefix) {
                throw new UnsupportedOperationException("Cannot encode with decode-only codec! Decoder:" + codec);
            }
        };
    });
    public static final ResourceLocation STP = ResourceLocation.fromNamespaceAndPath("confluence", "simple_template_piece");
    public static final ResourceLocation FIXED_STP = ConfluenceMagicLib.asResource("simple_template_piece");
    public static final ResourceLocation GP = ResourceLocation.fromNamespaceAndPath("confluence", "grid_piece");
    public static final ResourceLocation FIXED_GP = ConfluenceMagicLib.asResource("grid_piece");
    private static final Map<String, String> BLOCK_STATE_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:copper_coin_pile", "confluence:copper_coin")
            .put("confluence:silver_coin_pile", "confluence:silver_coin")
            .put("confluence:golden_coin_pile", "confluence:golden_coin")
            .put("confluence:platinum_coin_pile", "confluence:platinum_coin")
            .put("confluence:emerald_coin_pile", "confluence:emerald_coin")
            .build();
    private static final Map<String, String> ITEM_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:demon_ocnch", "confluence:demon_conch")
            .build();

    public static ResourceLocation fixPieceNamespace(ResourceLocation original) {
        if (original.equals(STP)) return FIXED_STP;
        if (original.equals(GP)) return FIXED_GP;
        return original;
    }

    public static <A> Codec.ResultFunction<A> fixBlockStateName(Codec<A> codec) {
        return new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
                if (a.isError()) {
                    MutableObject<DataResult<Pair<A, T>>> mutableObject = new MutableObject<>(a);
                    ops.getMap(input).ifSuccess(map -> {
                        T t = map.get("Name");
                        if (t != null) ops.getStringValue(t).ifSuccess(s -> {
                            String s1 = BLOCK_STATE_NAME_FIX_MAP.get(s);
                            if (s1 != null) ops.mergeToMap(input, ops.createString("Name"), ops.createString(s1))
                                    .ifSuccess(t1 -> mutableObject.setValue(codec.decode(ops, t1)));
                        });
                    });
                    return mutableObject.getValue();
                }
                return a;
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, A input, DataResult<T> t) {
                return t;
            }
        };
    }

    public static void fixPersistentData(Player player) {
        CompoundTag persistedData = LibUtils.getOrCreatePersistedData(player);
        if (persistedData.getBoolean("confluence:fixed_persistent_data")) return;
        CompoundTag persistentData = player.getPersistentData();
        persistentData.getAllKeys().removeIf(key -> {
            if (key.startsWith("confluence:") || key.startsWith("terra_curio:")) {
                Tag value = persistentData.get(key);
                if (value != null) persistedData.put(key, value);
                return true;
            }
            return false;
        });
        persistedData.putBoolean("confluence:fixed_persistent_data", true);
    }

    public static ResourceLocation fixPatchKeyNamespace(ResourceLocation original) {
        if (original.getNamespace().equals("terra_curio")) {
            String path = original.getPath();
            if (path.equals("nbt") || path.equals("tool_mode") || path.equals("mod_rarity")) {
                return ConfluenceMagicLib.asResource(path);
            }
        }
        return original;
    }

    public static Codec<Holder<Item>> fixItemName(Codec<Holder<Item>> codec) {
        return codec.mapResult(new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<Holder<Item>, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<Holder<Item>, T>> a) {
                if (a.isError()) {
                    return ops.getStringValue(input).result().map(s -> {
                        String s1 = ITEM_NAME_FIX_MAP.get(s);
                        return s1 == null ? a : codec.decode(ops, ops.createString(s1));
                    }).orElse(a);
                }
                return a;
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, Holder<Item> input, DataResult<T> t) {
                return t;
            }
        });
    }
}
