package org.confluence.lib.common.worldgen.biome;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.Arrays;

/// 一个 `LevelChunkSection`（16×16×16 = 4096 块）内的计数器快照。
///
/// 字段按 {@link BlockCounters} 的注册顺序索引，所以第三方模组可以在不改这个类的前提下
/// 添加自己的计数器 —— 这是它取代原先「固定 15 个 short 字段」版本的原因。
///
/// **不要**共享实例：计数是可变的，写入方拿到的就是这个对象。
public final class BlockCounts {
    private final short[] counts;
    private Int2ObjectOpenHashMap<byte[]> cells;
    private boolean cellsReady;
    private long revision;

    public BlockCounts() {
        BlockCounters.freeze();
        this.counts = new short[BlockCounters.size()];
    }

    public int size() {
        return counts.length;
    }

    public int get(int index) {
        return counts[index];
    }

    public void add(int index, int delta) {
        counts[index] += (short) delta;
        revision++;
    }

    public void set(int index, int value) {
        counts[index] = (short) value;
        revision++;
    }

    /// 清零。完整重算（`LevelChunkSection#recalcBlockCounts`）前必须调用，
    /// 因为计数器是「累加」语义，而重算流程可能对同一个 section 跑多次。
    public void clear() {
        java.util.Arrays.fill(counts, (short) 0);
        cells = null;
        cellsReady = false;
        revision++;
    }

    public long revision() {return revision;}

    /// 首次局部查询才构建分布；普通石头、空气等无匹配 section 不扫描、不分配小单元数组。
    public void prepareCells(LevelChunkSection section) {
        if (cellsReady) return;
        cellsReady = true;
        if (!BlockCounters.hasSpatialCounts(this)) return;
        cells = new Int2ObjectOpenHashMap<>();
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int cell = cellIndex(x >> 2, y >> 2, z >> 2);
                    for (int counter : BlockCounters.matched(section.getBlockState(x, y, z))) {
                        if (BlockCounters.isSpatial(counter))
                            cells.computeIfAbsent(counter, ignored -> new byte[64])[cell]++;
                    }
                }
            }
        }
    }

    /// 总数更新后同步已有分布；尚未查询过的 section 留到首次查询时从实际方块构建。
    public void changeCell(BlockState before, BlockState after, int x, int y, int z) {
        if (!cellsReady) return;
        int[] from = BlockCounters.matched(before);
        int[] to = BlockCounters.matched(after);
        int cell = cellIndex(x >> 2, y >> 2, z >> 2);
        for (int counter : from) {
            if (!BlockCounters.isSpatial(counter) || Arrays.binarySearch(to, counter) >= 0)
                continue;
            if (cells != null) {
                byte[] values = cells.get(counter);
                if (values != null) values[cell]--;
                if (counts[counter] == 0) cells.remove(counter);
            }
        }
        for (int counter : to) {
            if (!BlockCounters.isSpatial(counter) || Arrays.binarySearch(from, counter) >= 0)
                continue;
            if (cells == null) cells = new Int2ObjectOpenHashMap<>();
            cells.computeIfAbsent(counter, ignored -> new byte[64])[cell]++;
        }
        if (cells != null && cells.isEmpty()) cells = null;
    }

    public int cell(int counter, int x, int y, int z) {
        byte[] values = cells == null ? null : cells.get(counter);
        return values == null ? 0 : values[cellIndex(x, y, z)];
    }

    private static int cellIndex(int x, int y, int z) {
        return (y << 4) | (z << 2) | x;
    }
}
