package org.confluence.lib.common.worldgen.biome;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/// 迷你生物群系：叠加在原版群系之上、**不影响**原版群系的「标记」。
///
/// ## 判定模型（照搬泰拉瑞亚 `SceneMetrics`）
///
/// 判定范围是**以查询点为中心的一个窗口**内的方块计数，而不是「某一个方块 / 某一个区块属于谁」：
///
/// - 每种标记可声明独立窗口；同位置同范围共享计数缓存，
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
/// 缓存失效时才重新求和；同 tick 复用结果，下一个 tick 再检查计数版本。
/// 所以**不要**每个实体每 tick 调用。做法参照泰拉：每个玩家每隔若干 tick 算一次并缓存，
/// 实体 / 刷怪读缓存。{@link #windowCounts} 可以直接拿来自己缓存。
public final class MiniBiome {
    /// 判定窗口的水平半径（方块）。泰拉由屏幕尺寸派生，这里取一个固定值。
    public static final int WINDOW_RADIUS = 64;
    /// 判定窗口的垂直半高（方块）。
    public static final int WINDOW_HALF_HEIGHT = 48;

    private static final List<MiniBiomeType> TYPES = new ArrayList<>();
    /// 每个世界独立且容量有界；同一个 4 格查询位置和范围共用计数，不缓存 NPC provider 的结果。
    private static final Map<LevelAccessor, Map<Window, CachedCounts>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());
    private static final int CACHE_LIMIT = 256;
    private static final int CACHE_LIFETIME = 200;

    private MiniBiome() {}

    /// 注册一种迷你群系。建议在模组初始化阶段完成；注册表按 `priority` 升序维护。
    public static void register(MiniBiomeType type) {
        TYPES.add(type);
        TYPES.sort(Comparator.comparingInt(MiniBiomeType::priority));
    }

    /// 已注册的全部迷你群系，按 `priority` 升序。
    public static List<MiniBiomeType> types() {
        return TYPES;
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
        List<Marker> markers = new ArrayList<>(2);
        for (MiniBiomeType type : TYPES) {
            Marker marker = markerAt(level, pos, type);
            if (marker != null) markers.add(marker);
        }
        return markers;
    }

    /// 唯一答案：优先级最高（`priority` 最小）的那个标记，无命中返回 `null`。
    public static @Nullable Marker primaryAt(LevelAccessor level, BlockPos pos) {
        List<Marker> markers = markersAt(level, pos);
        return markers.isEmpty() ? null : markers.get(0);
    }

    /// 查询指定环境，不受其他同时命中的环境及其表现优先级影响。
    public static @Nullable Marker markerAt(LevelAccessor level, BlockPos pos, MiniBiomeType type) {
        if (!BlockCounters.isEnabled() || !type.allows(level, pos)) return null;
        int[] counts = type.usesBlocks() ? windowCounts(level, pos, type.horizontalRadius(), type.verticalRadius()) : null;
        int count = type.count(level, pos, counts);
        return type.matches(count) ? new Marker(type.id(), count, type.threshold(), type.max(), type.priority()) : null;
    }

    /// 是否处于指定迷你群系内。交错场景下「是否在 X 内」与「是不是 X」是两回事。
    public static boolean isInside(LevelAccessor level, BlockPos pos, ResourceLocation id) {
        MiniBiomeType type = type(id);
        return type != null && markerAt(level, pos, type) != null;
    }

    /// 窗口内所有计数器的求和（把窗口覆盖到的每个 section 的 {@link BlockCounts} 相加）。
    ///
    /// 未加载的区块会被跳过（用 `hasChunk` 判断，不会强制加载）。需要自己缓存的消费方
    /// 可以直接用这个：拿到的数组按 {@link BlockCounters} 的注册顺序索引。
    public static int[] windowCounts(LevelAccessor level, BlockPos pos) {
        return windowCounts(level, pos, WINDOW_RADIUS, WINDOW_HALF_HEIGHT);
    }

    /// 主线程查询；返回共享只读计数。4 格单元边缘裁切，内部整段直接使用总计数。
    public static int[] windowCounts(LevelAccessor level, BlockPos pos, int horizontalRadius, int verticalRadius) {
        Window window = new Window(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2, horizontalRadius, verticalRadius);
        Map<Window, CachedCounts> cache = CACHE.computeIfAbsent(level, ignored -> new LinkedHashMap<>(16, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Window, CachedCounts> entry) {
                return size() > CACHE_LIMIT;
            }
        });
        long tick = level.getLevelData().getGameTime();
        CachedCounts existing = cache.get(window);
        if (existing != null && existing.valid(tick)) return existing.sums;
        int[] sums = new int[BlockCounters.size()];
        List<Dependency> dependencies = new ArrayList<>();
        int minX = (window.x << 2) - horizontalRadius;
        int maxX = (window.x << 2) + horizontalRadius + 3;
        int minZ = (window.z << 2) - horizontalRadius;
        int maxZ = (window.z << 2) + horizontalRadius + 3;
        int minY = (window.y << 2) - verticalRadius;
        int maxY = (window.y << 2) + verticalRadius + 3;
        if (minY < level.getMinBuildHeight()) minY = level.getMinBuildHeight();
        if (maxY >= level.getMaxBuildHeight()) maxY = level.getMaxBuildHeight() - 1;
        for (int chunkX = minX >> 4; chunkX <= maxX >> 4; chunkX++)
            for (int chunkZ = minZ >> 4; chunkZ <= maxZ >> 4; chunkZ++) {
                if (!level.hasChunk(chunkX, chunkZ)) continue;
                ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
                for (int sy = minY >> 4; sy <= maxY >> 4; sy++) {
                    LevelChunkSection section = chunk.getSection(level.getSectionIndexFromSectionY(sy));
                    BlockCounts counts = ILevelChunkSection.of(section).confluence$getBlockCounts();
                    dependencies.add(new Dependency(counts, counts.revision()));
                    int x0 = minX > (chunkX << 4) ? minX - (chunkX << 4) : 0;
                    int x1 = maxX < (chunkX << 4) + 15 ? maxX - (chunkX << 4) : 15;
                    int z0 = minZ > (chunkZ << 4) ? minZ - (chunkZ << 4) : 0;
                    int z1 = maxZ < (chunkZ << 4) + 15 ? maxZ - (chunkZ << 4) : 15;
                    int y0 = minY > (sy << 4) ? minY - (sy << 4) : 0;
                    int y1 = maxY < (sy << 4) + 15 ? maxY - (sy << 4) : 15;
                    addBox(section, counts, sums, x0, y0, z0, x1, y1, z1);
                }
            }
        cache.put(window, new CachedCounts(sums, dependencies, tick));
        return sums;
    }

    private static void addBox(LevelChunkSection section, BlockCounts counts, int[] sums,
                               int x0, int y0, int z0, int x1, int y1, int z1) {
        if (x0 == 0 && y0 == 0 && z0 == 0 && x1 == 15 && y1 == 15 && z1 == 15) {
            for (int counter = 0; counter < sums.length; counter++)
                sums[counter] += counts.get(counter);
            return;
        }
        counts.prepareCells(section);
        boolean scanUnindexed = false;
        for (int counter = 0; counter < sums.length; counter++) {
            if (counts.get(counter) == 0) continue;
            if (!BlockCounters.isSpatial(counter)) {
                scanUnindexed = true;
                continue;
            }
            for (int y = y0 >> 2; y <= y1 >> 2; y++)
                for (int z = z0 >> 2; z <= z1 >> 2; z++)
                    for (int x = x0 >> 2; x <= x1 >> 2; x++)
                        sums[counter] += counts.cell(counter, x, y, z);
        }
        /// 保留公共 windowCounts 对未声明空间索引的第三方计数器的正确结果。
        if (scanUnindexed)
            for (int y = y0; y <= y1; y++)
                for (int z = z0; z <= z1; z++)
                    for (int x = x0; x <= x1; x++)
                        for (int counter : BlockCounters.matched(section.getBlockState(x, y, z)))
                            if (!BlockCounters.isSpatial(counter)) sums[counter]++;
    }

    /// 区块到达或卸载会改变窗口的可用范围，不能只依据方块 revision 判断缓存有效。
    public static void invalidate(LevelAccessor level) {
        CACHE.remove(level);
    }

    private record Window(int x, int y, int z, int horizontalRadius, int verticalRadius) {}

    private record Dependency(BlockCounts counts, long revision) {}

    private static final class CachedCounts {
        private final int[] sums;
        private final List<Dependency> dependencies;
        private final long createdTick;
        private long checkedTick;

        private CachedCounts(int[] sums, List<Dependency> dependencies, long tick) {
            this.sums = sums;
            this.dependencies = dependencies;
            createdTick = tick;
            checkedTick = tick;
        }

        private boolean valid(long tick) {
            if (tick == checkedTick) return true;
            if (tick < createdTick || tick - createdTick >= CACHE_LIFETIME) return false;
            for (Dependency dependency : dependencies)
                if (dependency.counts.revision() != dependency.revision) return false;
            checkedTick = tick;
            return true;
        }
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
