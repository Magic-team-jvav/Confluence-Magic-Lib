package org.confluence.lib.mixed;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.confluence.lib.common.worldgen.biome.BlockCounts;

/// `LevelChunkSection` 上的附加数据：方块计数器 + 纯净群系备份。
///
/// 由 `org.confluence.lib.mixin.chunk.LevelChunkSectionMixin` 实现。
/// 计数器需要消费方调用 `BlockCounters.enable()`（通常经各级 DynamicBiomeUtils 门面）后才会维护。
public interface ILevelChunkSection {
    BlockCounts confluence$getBlockCounts();

    /// 计数器是否已经反映当前方块内容（即自上次完整重算以来没有发生**未经追踪**的方块改动）。
    ///
    /// 读档路径（`LevelChunkSection(PalettedContainer, PalettedContainerRO)` 构造器）里 vanilla 会完整
    /// 重算一次，所以那种 section 一开始就是新鲜的；而世界生成写进 ProtoChunk 的方块不经过增量钩子，
    /// 所以生成路径上的 section 必须由 `applyDynamicBiome` 再完整重算一次。
    boolean confluence$isCountsFresh();

    void confluence$setCountsFresh(boolean fresh);

    PalettedContainerRO<Holder<Biome>> confluence$getBackupBiome();

    void confluence$setBackupBiome(PalettedContainerRO<Holder<Biome>> biome);

    void confluence$setBiomes(PalettedContainerRO<Holder<Biome>> biomes);

    static ILevelChunkSection of(LevelChunkSection section) {
        return (ILevelChunkSection) section;
    }
}
