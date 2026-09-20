package org.confluence.lib.common.worldgen.biome;

/// 一个 `LevelChunkSection`（16×16×16 = 4096 块）内的计数器快照。
///
/// 字段按 {@link BlockCounters} 的注册顺序索引，所以第三方模组可以在不改这个类的前提下
/// 添加自己的计数器 —— 这是它取代原先「固定 15 个 short 字段」版本的原因。
///
/// **不要**共享实例：计数是可变的，写入方拿到的就是这个对象。
public final class BlockCounts {
    private final short[] counts;

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
    }

    public void set(int index, int value) {
        counts[index] = (short) value;
    }

    /// 清零。完整重算（`LevelChunkSection#recalcBlockCounts`）前必须调用，
    /// 因为计数器是「累加」语义，而重算流程可能对同一个 section 跑多次。
    public void clear() {
        java.util.Arrays.fill(counts, (short) 0);
    }
}
