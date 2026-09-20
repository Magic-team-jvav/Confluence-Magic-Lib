package org.confluence.lib.common.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// 迷你生物群系：叠加在原版群系之上、**不影响**原版群系的「标记」。
///
/// ## 判定模型（照搬泰拉瑞亚 `SceneMetrics`）
///
/// 判定范围是**以查询点为中心的一个窗口**内的方块计数，而不是「某一个方块 / 某一个区块属于谁」：
///
/// - 窗口是全局的（见 {@link #WINDOW_RADIUS} / {@link #WINDOW_HALF_HEIGHT}），
///   泰拉用的是 `Main.buffScanAreaWidth × buffScanAreaHeight`（约 169×116 格）。
///   所以「离微光池中心有一定距离也能判定成微光」来自窗口足够大，而不是空间渐变。
/// - 每种迷你群系 = 若干计数器按权重累加后与自己的阈值比较（{@link MiniBiomeType}）。
/// - 多个迷你群系可以**同时命中**（泰拉的 hybrid biome / 交错），
///   {@link #markersAt} 返回全部，{@link #primaryAt} 才用 `priority` 仲裁唯一答案。
/// - **强度（渐变）**是计数的归一化（`count` 在 `[threshold, max]` 之间），
///   对应泰拉的 `count / Max` 与 `Utils.GetLerpValue`。
///
/// ## 数据来源
///
/// 计数来自每个 `LevelChunkSection` 上增量维护的 {@link BlockCounts}
/// （机制见 {@link BlockCounters}，需要消费方调用 `DynamicBiomeUtils.enable()`）；
/// 非方块来源（例如按 region 统计 NPC 数量的小镇）用 {@link MiniBiomeType.Provider}。
///
/// ## 开销
///
/// 一次查询要遍历窗口覆盖到的所有 section（默认半径 64、半高 48 ≈ 567 个 section），
/// 所以**不要**每个实体每 tick 调用。做法参照泰拉：每个玩家每隔若干 tick 算一次并缓存，
/// 实体 / 刷怪读缓存。{@link #windowCounts} 可以直接拿来自己缓存。
public final class MiniBiome {
    /// 判定窗口的水平半径（方块）。泰拉由屏幕尺寸派生，这里取一个固定值。
    public static final int WINDOW_RADIUS = 64;
    /// 判定窗口的垂直半高（方块）。
    public static final int WINDOW_HALF_HEIGHT = 48;

    private static final List<MiniBiomeType> TYPES = new ArrayList<>();

    private MiniBiome() {}

    /// 注册一种迷你群系。建议在模组初始化阶段完成；注册表按 `priority` 升序维护。
    public static void register(MiniBiomeType type) {
        TYPES.add(type);
        TYPES.sort(Comparator.comparingInt(MiniBiomeType::priority));
    }

    /// 已注册的全部迷你群系，按 `priority` 升序。
    public static List<MiniBiomeType> types() {
        return List.copyOf(TYPES);
    }

    public static @Nullable MiniBiomeType type(ResourceLocation id) {
        for (MiniBiomeType type : TYPES) {
            if (type.id().equals(id)) return type;
        }
        return null;
    }

    /// 该坐标命中的**全部**标记，按 `priority` 升序（数字小的优先级高）。
    ///
    /// 未启用计数机制或没有注册任何迷你群系时返回空表。
    public static List<Marker> markersAt(LevelAccessor level, BlockPos pos) {
        if (TYPES.isEmpty() || !BlockCounters.isEnabled()) return List.of();
        int[] windowCounts = windowCounts(level, pos);
        List<Marker> markers = new ArrayList<>(2);
        for (MiniBiomeType type : TYPES) {
            int count = type.count(level, pos, windowCounts);
            if (type.matches(count)) {
                markers.add(new Marker(type.id(), count, type.threshold(), type.max(), type.priority()));
            }
        }
        return markers;
    }

    /// 唯一答案：优先级最高（`priority` 最小）的那个标记，无命中返回 `null`。
    public static @Nullable Marker primaryAt(LevelAccessor level, BlockPos pos) {
        List<Marker> markers = markersAt(level, pos);
        return markers.isEmpty() ? null : markers.get(0);
    }

    /// 是否处于指定迷你群系内。交错场景下「是否在 X 内」与「是不是 X」是两回事。
    public static boolean isInside(LevelAccessor level, BlockPos pos, ResourceLocation id) {
        for (Marker marker : markersAt(level, pos)) {
            if (marker.id().equals(id)) return true;
        }
        return false;
    }

    /// 窗口内所有计数器的求和（把窗口覆盖到的每个 section 的 {@link BlockCounts} 相加）。
    ///
    /// 未加载的区块会被跳过（用 `hasChunk` 判断，不会强制加载）。需要自己缓存的消费方
    /// 可以直接用这个：拿到的数组按 {@link BlockCounters} 的注册顺序索引。
    public static int[] windowCounts(LevelAccessor level, BlockPos pos) {
        int[] sums = new int[BlockCounters.size()];
        int minChunkX = (pos.getX() - WINDOW_RADIUS) >> 4;
        int maxChunkX = (pos.getX() + WINDOW_RADIUS) >> 4;
        int minChunkZ = (pos.getZ() - WINDOW_RADIUS) >> 4;
        int maxChunkZ = (pos.getZ() + WINDOW_RADIUS) >> 4;
        int minY = pos.getY() - WINDOW_HALF_HEIGHT;
        int maxY = pos.getY() + WINDOW_HALF_HEIGHT;

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                int sectionsCount = chunk.getSectionsCount();
                int from = Mth.clamp(level.getSectionIndex(minY), 0, sectionsCount - 1);
                int to = Mth.clamp(level.getSectionIndex(maxY), 0, sectionsCount - 1);
                for (int index = from; index <= to; index++) {
                    LevelChunkSection section = chunk.getSection(index);
                    BlockCounts counts = ILevelChunkSection.of(section).confluence$getBlockCounts();
                    for (int counter = 0; counter < sums.length; counter++) {
                        sums[counter] += counts.get(counter);
                    }
                }
            }
        }
        return sums;
    }

    /// 一次命中结果。
    ///
    /// @param count     加权计数（窗口求和的结果）
    /// @param threshold 判定阈值（`count >= threshold` 才算命中）
    /// @param max       强度到达 1.0 的计数；等于 `threshold` 时即为布尔化
    /// @param priority  优先级，数字小的优先
    public record Marker(ResourceLocation id, int count, int threshold, int max, int priority) {
        public float influence() {
            return MiniBiome.influence(count, threshold, max);
        }
    }

    // ------------------------------------------------------------------
    // 强度与平滑
    // ------------------------------------------------------------------

    /// 计数 -> `[0, 1]` 强度：`count <= min` 为 0，`count >= max` 为 1，中间线性。
    ///
    /// 对应泰拉的 `Utils.GetLerpValue(min, max, count, true)`。
    public static float influence(int count, int min, int max) {
        if (max <= min) return count >= max ? 1.0F : 0.0F;
        if (count <= min) return 0.0F;
        if (count >= max) return 1.0F;
        return (float) (count - min) / (float) (max - min);
    }

    /// 强度的时间平滑，避免表现随计数抖动而瞬变。
    ///
    /// 对应泰拉对蘑菇光强度的处理（每帧按步长缓动，然后 clamp 到 `[0, 1]`）。
    public static float smooth(float current, float target, float stepUp, float stepDown) {
        float next = current < target
                ? Math.min(target, current + stepUp)
                : Math.max(target, current - stepDown);
        return Mth.clamp(next, 0.0F, 1.0F);
    }

    // ------------------------------------------------------------------
    // 微光（Shimmer）
    // ------------------------------------------------------------------
    //
    // 微光是**渐变**，不是布尔：判定用窗口计数，表现用归一化强度 + 时间平滑。
    //
    // 注意：本地那份泰拉源码是 1.4.0.5，其中没有微光（1.4.4 才加入），
    // 所以下面两个数值是占位值，直接改这里即可，不影响其它逻辑。

    /// 强度起点：窗口内微光计数低于此值视为 0。
    public static final int SHIMMER_COUNT_MIN = 40;
    /// 强度达到 1.0 的计数。
    public static final int SHIMMER_COUNT_MAX = 400;
    /// 强度上升 / 下降的时间平滑步长。
    public static final float SHIMMER_STEP_UP = 0.01F;
    public static final float SHIMMER_STEP_DOWN = 0.02F;

    /// 微光强度。
    public static float shimmerInfluence(int count) {
        return influence(count, SHIMMER_COUNT_MIN, SHIMMER_COUNT_MAX);
    }
}
