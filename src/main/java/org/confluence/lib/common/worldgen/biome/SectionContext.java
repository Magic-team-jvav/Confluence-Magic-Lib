package org.confluence.lib.common.worldgen.biome;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.confluence.lib.mixed.ILevelChunkSection;

import java.util.function.Predicate;

/// 动态群系判定规则的上下文：section 本体、它的方块计数、以及群系注册表查找。
public record SectionContext(
        LevelChunkSection section,
        BlockCounts counts,
        HolderLookup.RegistryLookup<Biome> lookup,
        Holder<Biome> originalBiome,
        Holder<Biome> currentBiome
) {
    public SectionContext(LevelChunkSection section, BlockCounts counts, HolderLookup.RegistryLookup<Biome> lookup) {
        this(section, counts, lookup, ILevelChunkSection.of(section).confluence$getBackupBiome().get(0, 0, 0), section.getBiomes().get(0, 0, 0));
    }
    /// 该 section 原有的群系是否满足条件（原版群系被替换前保留的信息）
    public boolean originalBiomeIs(Predicate<Holder<Biome>> predicate) {
        return predicate.test(originalBiome);
    }
}
