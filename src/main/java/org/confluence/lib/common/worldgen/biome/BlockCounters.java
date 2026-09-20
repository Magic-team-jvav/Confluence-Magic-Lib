package org.confluence.lib.common.worldgen.biome;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/// 方块计数器的注册表（开放 API）。
///
/// 一个计数器 = 「方块谓词 → 某个计数字段」的绑定。每个 `LevelChunkSection` 持有一份
/// {@link BlockCounts}（`short[]`，按注册顺序索引），在方块变化时增量维护，用来推导
/// 动态群系与迷你生物群系标记（泰拉瑞亚的 `SceneMetrics` 是同一套思路）。
///
/// ## 注册时机
///
/// 必须在**模组初始化阶段**完成注册：计数数组长度在第一个 section 创建时固化，
/// 之后再注册会抛异常（避免运行期静默错位，比默默读错字段好排查）。
///
/// ## 开关
///
/// 需要用这套机制的模组调用 {@link #enable()}，通常在模组构造函数里、任何世界加载之前。
/// 未启用时注入点直接返回：不计数、不分配 `BlockCounts`。
public final class BlockCounters {
    private static final List<Entry> ENTRIES = new ArrayList<>();
    /// `BlockState` 没有覆写 equals/hashCode（即引用语义），且实例是 interned 的，
    /// 所以按状态缓存「命中了哪些计数器」成立，并且可以用并发 map 供区块生成线程使用。
    private static final Map<BlockState, int[]> MATCHED = new ConcurrentHashMap<>();
    private static final int[] NONE = new int[0];

    private static volatile boolean frozen;
    private static volatile boolean enabled;

    private BlockCounters() {}

    public static void enable() {
        enabled = true;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /// @param predicate 该方块是否计入此计数器
    public static synchronized Counter register(ResourceLocation id, Predicate<BlockState> predicate) {
        if (frozen) {
            throw new IllegalStateException("Block count array is fixed: register counters during mod initialization, before any chunk is created. id=" + id);
        }
        Counter counter = new Counter(id, ENTRIES.size());
        ENTRIES.add(new Entry(id, predicate, counter));
        return counter;
    }

    public static int size() {
        return ENTRIES.size();
    }

    /// 冻结注册表。由 {@link BlockCounts} 首次分配时调用。
    static synchronized void freeze() {
        frozen = true;
    }

    /// 该方块状态命中的计数器索引（升序），结果按状态缓存。
    ///
    /// 取代「每次方块变化都遍历整张注册表跑一遍谓词」：方块变更的成本从
    /// `O(注册表大小)` 降到 `O(命中数)`，未命中时是零。
    public static int[] matched(BlockState state) {
        int[] cached = MATCHED.get(state);
        if (cached != null) return cached;

        IntArrayList indices = new IntArrayList();
        int size = ENTRIES.size();
        for (int i = 0; i < size; i++) {
            if (ENTRIES.get(i).predicate.test(state)) indices.add(i);
        }
        int[] result = indices.isEmpty() ? NONE : indices.toIntArray();
        MATCHED.put(state, result);
        return result;
    }

    /// 把 `count` 份该方块计入对应计数器（用于读档时按调色板批量重算）。
    public static void add(BlockState state, int count, BlockCounts counts) {
        for (int index : matched(state)) {
            counts.add(index, count);
        }
    }

    /// 方块从 `before` 变成 `after` 时维护计数。
    public static void applyChange(@Nullable BlockState before, @Nullable BlockState after, BlockCounts counts) {
        int[] from = before == null ? NONE : matched(before);
        int[] to = after == null ? NONE : matched(after);
        if (from == to) return;
        for (int index : from) {
            if (!contains(to, index)) counts.add(index, -1);
        }
        for (int index : to) {
            if (!contains(from, index)) counts.add(index, 1);
        }
    }

    private static boolean contains(int[] array, int value) {
        for (int i : array) {
            if (i == value) return true;
        }
        return false;
    }

    public record Entry(ResourceLocation id, Predicate<BlockState> predicate, Counter counter) {}

    /// 计数器句柄。按注册顺序索引，指向所属 section 的 {@link BlockCounts}。
    public static final class Counter {
        private final ResourceLocation id;
        private final int index;

        Counter(ResourceLocation id, int index) {
            this.id = id;
            this.index = index;
        }

        public ResourceLocation id() {
            return id;
        }

        public int index() {
            return index;
        }

        /// 读取某个 section 的该计数。
        public int get(BlockCounts counts) {
            return counts.get(index);
        }
    }
}
