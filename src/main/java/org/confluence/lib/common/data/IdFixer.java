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
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
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
                if (result.isEmpty()) return DataResult.error(() -> "Error decoding!");
                List<Tuple<Integer, LongArrayList>> list = result.get();
                List<Tuple<Integer, IntArrayList>> ret = new ArrayList<>(5 + list.size() + (list.size() / 10));
                for (Tuple<Integer, LongArrayList> tuple : list) {
                    LongArrayList longs = tuple.getB();
                    IntArrayList integers = new IntArrayList(5 + longs.size() + (longs.size() / 10));
                    for (long l : longs) {
                        integers.add(((BlockPos.getX(l) & 0xF) << 16) | ((BlockPos.getY(l) + 2048) << 4) | (BlockPos.getZ(l) & 0xF));
                    }
                    ret.add(new Tuple<>(tuple.getA(), integers));
                }
                return DataResult.success(new Pair<>(ret, input), Lifecycle.stable());
            }

            @Override
            public <T> DataResult<T> encode(List<Tuple<Integer, IntArrayList>> input, DynamicOps<T> ops, T prefix) {
                throw new UnsupportedOperationException("Cannot encode with decode-only codec! Decoder:" + codec);
            }
        };
    });
    public static final ResourceLocation STP = ResourceLocation.fromNamespaceAndPath(ConfluenceMagicLib.CONFLUENCE_ID, "simple_template_piece");
    public static final ResourceLocation FIXED_STP = ConfluenceMagicLib.asResource("simple_template_piece");
    public static final ResourceLocation GP = ResourceLocation.fromNamespaceAndPath(ConfluenceMagicLib.CONFLUENCE_ID, "grid_piece");
    public static final ResourceLocation FIXED_GP = ConfluenceMagicLib.asResource("grid_piece");
    private static final Map<String, String> BLOCK_WITH_ITEM_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:freeze_crate", "confluence:frozen_crate")
            .put("confluence:lvy_chest", "confluence:ivy_chest")

            .put("confluence:ebony_stone", "confluence:ebonstone")
            .put("confluence:pearl_stone", "confluence:pearlstone")
            .put("confluence:tr_crimson_stone", "confluence:crimstone")
            .put("confluence:ebony_cobblestone", "confluence:cobbled_ebonstone")
            .put("confluence:pearl_cobblestone", "confluence:cobbled_pearlstone")
            .put("confluence:tr_crimson_cobblestone", "confluence:cobbled_crimstone")
            .put("confluence:ebony_sandstone", "confluence:ebonsandstone")
            .put("confluence:tr_crimson_sandstone", "confluence:crimsandstone")
            .put("confluence:pearl_sandstone", "confluence:pearlsandstone")
            .put("confluence:ebony_sand", "confluence:ebonsand")
            .put("confluence:pearl_sand", "confluence:pearlsand")
            .put("confluence:crimson_sand", "confluence:crimsand")
            .put("confluence:ebony_sand_layer_block", "confluence:ebonsand_layer_block")
            .put("confluence:tr_crimson_sand_layer_block", "confluence:crimsand_layer_block")
            .put("confluence:pearl_sand_layer_block", "confluence:pearlsand_layer_block")
            .put("confluence:red_hardened_sand_block", "confluence:hardened_red_sand_block")
            .put("confluence:ebony_hardened_sand_block", "confluence:hardened_ebonsand_block")
            .put("confluence:pearl_hardened_sand_block", "confluence:hardened_pearlsand_block")
            .put("confluence:tr_crimson_hardened_sand_block", "confluence:hardened_crimsand_block")
            .put("confluence:ebony_moist_sand_block", "confluence:moistened_ebonsand_block")
            .put("confluence:pearl_moist_sand_block", "confluence:moistened_pearlsand_block")
            .put("confluence:tr_crimson_moist_sand_block", "confluence:moistened_crimsand_block")
            .put("confluence:moist_sand_block", "confluence:moistened_sand_block")
            .put("confluence:red_moist_sand_block", "confluence:moistened_red_sand_block")

            .put("confluence:tr_lava_bricks", "confluence:hellstone_bricks")

            .put("confluence:tr_amethyst_ore", "confluence:amethyst_ore")
            .put("confluence:deepslate_tr_amethyst_ore", "confluence:deepslate_amethyst_ore")
            .put("confluence:sanctification_tr_amethyst_ore", "confluence:sanctification_amethyst_ore")
            .put("confluence:corruption_tr_amethyst_ore", "confluence:corruption_amethyst_ore")
            .put("confluence:fleshification_tr_amethyst_ore", "confluence:fleshification_amethyst_ore")

            .put("confluence:tr_crimson_ore", "confluence:crimtane_ore")
            .put("confluence:deepslate_tr_crimson_ore", "confluence:deepslate_crimtane_ore")
            .put("confluence:sanctification_tr_crimson_ore", "confluence:sanctification_crimtane_ore")
            .put("confluence:corruption_tr_crimson_ore", "confluence:corruption_crimtane_ore")
            .put("confluence:fleshification_tr_crimson_ore", "confluence:fleshification_crimtane_ore")
            .put("confluence:raw_tr_crimson_block", "confluence:raw_crimtane_block")
            .put("confluence:tr_crimson_block", "confluence:crimtane_block")

            .put("confluence:tr_crimson_grass_block", "confluence:crimson_grass_block")
            .put("confluence:tr_crimson_jungle_grass_block", "confluence:crimson_jungle_grass_block")
            .put("confluence:tr_crimson_drooping_vine", "confluence:crimson_drooping_vine")
            .put("confluence:tr_crimson_grass", "confluence:crimson_grass")
            .put("confluence:tr_crimson_cattails_body", "confluence:crimson_cattails_body")
            .put("confluence:tr_crimson_cattails_head", "confluence:crimson_cattails_head")
            .put("confluence:tr_crimson_pot", "confluence:crimson_pot")
            .put("confluence:tr_crimson_crate", "confluence:crimson_crate")
            .put("confluence:tr_crimson_cattails", "confluence:crimson_cattails")

            .put("confluence:tr_crimson_ore_bricks", "confluence:crimtane_ore_bricks")
            .put("confluence:tr_crimson_rock_bricks", "confluence:crimstone_bricks")

            .put("confluence:tr_amethyst_branches", "confluence:amethyst_branches")
            .put("confluence:tr_amethyst_sapling", "confluence:amethyst_sapling")


            .put("confluence:tr_amethyst_block", "confluence:amethyst_block")
            .put("confluence:tr_polished_granite", "confluence:polished_granite")
            .put("confluence:tr_copper_bricks", "confluence:copper_bricks")

            .put("confluence:tr_gold_bricks", "confluence:golden_bricks")
            .put("confluence:tr_iron_bricks", "confluence:iron_bricks")
            .put("confluence:ebony_rock_bricks", "confluence:ebonstone_bricks")
            .put("confluence:tr_obsidian_bricks", "confluence:obsidian_bricks")
            .put("confluence:tr_obsidian_small_bricks", "confluence:obsidian_small_bricks")
            .put("confluence:tr_smooth_obsidian", "confluence:smooth_obsidian")
            .put("confluence:tr_oak_planks", "confluence:chiseled_oak_planks")
            .put("confluence:tr_northland_planks", "confluence:chiseled_spruce_planks")
            .put("confluence:tr_granite_column", "confluence:granite_column")

            .put("confluence:tr_emerald_ore", "confluence:jade_ore")
            .put("confluence:deepslate_tr_emerald_ore", "confluence:deepslate_jade_ore")
            .put("confluence:sanctification_tr_emerald_ore", "confluence:sanctification_jade_ore")
            .put("confluence:corruption_tr_emerald_ore", "confluence:corruption_jade_ore")
            .put("confluence:fleshification_tr_emerald_ore", "confluence:fleshification_jade_ore")
            .put("confluence:tr_emerald_block", "confluence:jade_block")

            .put("confluence:emerald_branches", "confluence:jade_branches")
            .put("confluence:emerald_sapling", "confluence:jade_sapling")
            .put("confluence:emerald_chain", "confluence:jade_chain")

            .build();
    private static final Map<String, String> BLOCK_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:copper_coin_pile", "confluence:copper_coin")
            .put("confluence:silver_coin_pile", "confluence:silver_coin")
            .put("confluence:golden_coin_pile", "confluence:golden_coin")
            .put("confluence:platinum_coin_pile", "confluence:platinum_coin")
            .put("confluence:emerald_coin_pile", "confluence:emerald_coin")
            .putAll(BLOCK_WITH_ITEM_NAME_FIX_MAP)
            .build();
    private static final Map<String, String> ITEM_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:tr_crimson_ingot", "confluence:crimtane_ingot")
            .put("confluence:raw_tr_crimson", "confluence:raw_crimtane")

            .put("confluence:tr_emerald", "confluence:jade")
            .put("terra_entity:emerald_whip", "terra_entity:jade_whip")
            .put("confluence:emerald_minecart", "confluence:jade_minecart")
            .put("confluence:emerald_hook", "confluence:jade_hook")
            .put("confluence:emerald_staff", "confluence:jade_staff")

            .put("confluence:tr_amethyst", "confluence:amethyst")
            .put("confluence:tr_crimson_seed", "confluence:crimson_seed")
            .put("confluence:tr_clownfish", "confluence:clownfish")
            .put("confluence:tr_salmon", "confluence:salmon")
            .put("confluence:red_light_saber", "confluence:red_phaseblade")
            .put("confluence:orange_light_saber", "confluence:orange_phaseblade")
            .put("confluence:yellow_light_saber", "confluence:yellow_phaseblade")
            .put("confluence:green_light_saber", "confluence:green_phaseblade")
            .put("confluence:blue_light_saber", "confluence:blue_phaseblade")
            .put("confluence:purple_light_saber", "confluence:purple_phaseblade")
            .put("confluence:white_light_saber", "confluence:white_phaseblade")
            .put("confluence:demon_ocnch", "confluence:demon_conch")
            .putAll(BLOCK_WITH_ITEM_NAME_FIX_MAP)
            .build();
    private static final Map<String, String> BIOME_NAME_FIX_MAP = ImmutableMap.<String, String>builder()
            .put("confluence:tr_crimson", "confluence:the_crimson")
            .put("confluence:tr_crimson_desert", "confluence:the_crimson_desert")
            .put("confluence:tr_crimson_tundra", "confluence:the_crimson_tundra")
            .build();
    public static final Codec.ResultFunction<ResourceKey<Biome>> FIX_BIOME_KEY_FUNC = new Codec.ResultFunction<>() {
        @Override
        public <T> DataResult<Pair<ResourceKey<Biome>, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<ResourceKey<Biome>, T>> a) {
            return ops.getStringValue(input).result().map(s -> {
                String s1 = BIOME_NAME_FIX_MAP.get(s);
                try {
                    return s1 == null ? a : DataResult.success(new Pair<>(ResourceKey.create(Registries.BIOME, ResourceLocation.parse(s1)), input), Lifecycle.stable());
                } catch (Exception e) {
                    return a;
                }
            }).orElse(a);
        }

        @Override
        public <T> DataResult<T> coApply(DynamicOps<T> ops, ResourceKey<Biome> input, DataResult<T> t) {
            return t;
        }
    };

    public static ResourceLocation fixPieceNamespace(ResourceLocation original) {
        if (original.equals(STP)) return FIXED_STP;
        if (original.equals(GP)) return FIXED_GP;
        return original;
    }

    public static <A> Codec.ResultFunction<A> fixBlockName(Codec<A> codec) {
        return new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<A, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<A, T>> a) {
                if (a.isSuccess()) return a;
                MutableObject<DataResult<Pair<A, T>>> mutableObject = new MutableObject<>(a);
                ops.getMap(input).ifSuccess(map -> {
                    T t = map.get("Name");
                    if (t != null) ops.getStringValue(t).ifSuccess(s -> {
                        String s1 = BLOCK_NAME_FIX_MAP.get(s);
                        if (s1 != null) ops.mergeToMap(input, ops.createString("Name"), ops.createString(s1))
                                .ifSuccess(t1 -> mutableObject.setValue(codec.decode(ops, t1)));
                    });
                });
                return mutableObject.getValue();
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
                if (a.isSuccess()) return a;
                return ops.getStringValue(input).result().map(s -> {
                    String s1 = ITEM_NAME_FIX_MAP.get(s);
                    return s1 == null ? a : codec.decode(ops, ops.createString(s1));
                }).orElse(a);
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, Holder<Item> input, DataResult<T> t) {
                return t;
            }
        });
    }

    public static Codec.ResultFunction<Holder<Biome>> fixBiomeName(Codec<Holder<Biome>> codec) {
        return new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<Holder<Biome>, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<Holder<Biome>, T>> a) {
                if (a.isSuccess()) return a;
                return ops.getStringValue(input).result().map(s -> {
                    String s1 = BIOME_NAME_FIX_MAP.get(s);
                    return s1 == null ? a : codec.decode(ops, ops.createString(s1));
                }).orElse(a);
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, Holder<Biome> input, DataResult<T> t) {
                return t;
            }
        };
    }
}
