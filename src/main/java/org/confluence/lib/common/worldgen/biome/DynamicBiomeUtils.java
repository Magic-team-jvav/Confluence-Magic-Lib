package org.confluence.lib.common.worldgen.biome;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.confluence.lib.mixed.IPalettedContainer;
import org.confluence.lib.util.ReturnException;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/// 动态群系：按 section 内的方块计数把原版群系替换成消费方自己的群系
/// （Confluence 用它做腐化/猩红/神圣/发光蘑菇等；迷你生物群系标记也复用同一套计数）。
///
/// ## 机制在库、规则在消费方
///
/// 本类只提供机制：计数开关、规则注册、优先级、可蔓延判定与上下传播。
/// 「多少计数算腐化」「哪个群系优先」这些是消费方语义，通过
/// {@link #registerRule(BiomeRule)}、{@link #registerPriority(ResourceKey, int)}、
/// {@link #registerSpreadable(Predicate)} 注册进来，第三方模组可以照做。
///
/// ## 开关
///
/// 消费方在模组初始化阶段（任何世界加载之前）调用一次 {@link #enable()}；
/// 未启用时 mixins 在入口直接返回，计数器不分配。
public final class DynamicBiomeUtils {
    private static final List<BiomeRule> RULES = new ArrayList<>();
    /// 数字小的优先级高
    private static final Object2IntMap<ResourceKey<Biome>> PRIORITY = new Object2IntOpenHashMap<>();
    private static volatile Predicate<Holder<Biome>> spreadable = biome -> false;

    private DynamicBiomeUtils() {}

    public static void enable() {
        BlockCounters.enable();
    }

    public static boolean isEnabled() {
        return BlockCounters.isEnabled();
    }

    public static void registerRule(BiomeRule rule) {
        RULES.add(rule);
    }

    public static void registerPriority(ResourceKey<Biome> biome, int priority) {
        PRIORITY.put(biome, priority);
    }

    /// 注册「这个群系是可被蔓延/需要净化的邪恶或神圣群系」的判定。
    /// 用于区块备份群系（purify 时回滚到纯净形态）。
    public static void registerSpreadable(Predicate<Holder<Biome>> predicate) {
        spreadable = predicate;
    }

    public static boolean isSpreadable(Holder<Biome> biome) {
        return spreadable.test(biome);
    }

    /// 优先级，数字小的优先。未注册的一律最低。
    public static int priorityOf(Holder<Biome> biome) {
        ResourceKey<Biome> key = biome.unwrapKey().orElse(null);
        return key == null ? Integer.MAX_VALUE : PRIORITY.getInt(key);
    }

    /// 按注册顺序跑规则，第一条返回非 null 的生效。
    ///
    /// @return 该 section 应该变成的群系；没有规则命中返回 null（保持原版群系）
    public static @Nullable Holder<Biome> judgeSection(LevelChunkSection section, HolderLookup.RegistryLookup<Biome> lookup) {
        if (!BlockCounters.isEnabled() || RULES.isEmpty()) return null;
        SectionContext context = new SectionContext(section, ILevelChunkSection.of(section).confluence$getBlockCounts(), lookup);
        for (int i = 0; i < RULES.size(); i++) {
            Holder<Biome> result = RULES.get(i).test(context);
            if (result != null) return result;
        }
        return null;
    }

    /// 判断这个区块的纯净形态应该是什么样的。
    ///
    /// 如果原群系包含邪恶：如果纯邪恶则返回平原，否则从纯净中挑一个；
    /// 如果原群系没有邪恶则返回原群系。
    public static PalettedContainer<Holder<Biome>> judgeBackupBiome(LevelChunkSection section, HolderLookup.RegistryLookup<Biome> lookup) {
        PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
        AtomicReference<Holder<Biome>> pure = new AtomicReference<>();
        AtomicBoolean hasEvil = new AtomicBoolean(false);
        try {
            biomes.getAll(biome -> {
                if (isSpreadable(biome)) {
                    hasEvil.set(true);
                } else {
                    pure.set(biome);
                }
                if (pure.get() != null && hasEvil.get()) {
                    throw new ReturnException();
                }
            });
        } catch (ReturnException ignore) {
        }
        if (hasEvil.get()) {
            if (pure.get() == null) {
                return IPalettedContainer.recreateSingle(biomes, lookup.getOrThrow(Biomes.PLAINS));
            }
            return IPalettedContainer.recreateSingle(biomes, pure.get());
        }
        return biomes;
    }

    public static @Nullable LevelChunkSection getSection(LevelAccessor level, BlockPos pos) {
        if (level.isOutsideBuildHeight(pos)) return null;
        return level.getChunk(pos).getSection(level.getSectionIndex(pos.getY()));
    }

    public static @Nullable ILevelChunkSection getISection(LevelAccessor level, BlockPos pos) {
        LevelChunkSection section = getSection(level, pos);
        return section == null ? null : ILevelChunkSection.of(section);
    }

    /// 为这个区块应用动态群系，从底部判断到顶部。
    public static void applyDynamicBiome(ChunkAccess chunk, HolderLookup.RegistryLookup<Biome> lookup) {
        if (!isEnabled()) return;
        Holder<Biome> belowBiome = null;
        for (LevelChunkSection section : chunk.getSections()) {
            ILevelChunkSection iSection = ILevelChunkSection.of(section);
            // 读档路径在构造器里已经算过一遍，别重复数（见 ILevelChunkSection#confluence$isCountsFresh）；
            // 世界生成路径写进 ProtoChunk 的方块不经过增量钩子，必须在这里补一次。
            if (!iSection.confluence$isCountsFresh()) {
                section.recalcBlockCounts();
                iSection.confluence$setCountsFresh(true);
            }
            Holder<Biome> currentBiome = judgeSection(section, lookup);
            if (currentBiome != null) {
                iSection.confluence$setBiomes(IPalettedContainer.recreateSingle(section.getBiomes(), currentBiome));
            } else if (belowBiome != null) {
                iSection.confluence$setBiomes(IPalettedContainer.recreateSingle(section.getBiomes(), belowBiome));
            }
            belowBiome = currentBiome;
        }
    }
}
