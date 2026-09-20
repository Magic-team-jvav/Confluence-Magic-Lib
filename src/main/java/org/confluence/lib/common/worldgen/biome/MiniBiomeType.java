package org.confluence.lib.common.worldgen.biome;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;

/// 一种迷你生物群系的定义。
///
/// 判定模型见 {@link MiniBiome}：以查询点为中心的一个窗口内的方块计数（泰拉瑞亚 `SceneMetrics`
/// 的做法），达标即命中。每种迷你群系声明自己的窗口、附加条件、阈值与强度上限，
/// 以及由哪些计数器按什么权重累加（权重可以是负数，用来表达抵消，例如向日葵抵消墓碑）。
///
/// 除方块计数外，还可以用 {@link Provider} 接入别的数据来源（例如「小镇」按 region 内的 NPC 数量判定）。
public final class MiniBiomeType {
    private final ResourceLocation id;
    private final int priority;
    private final int threshold;
    private final int max;
    private final Object2IntMap<BlockCounters.Counter> contributions;
    private final @Nullable Provider provider;
    private final int horizontalRadius;
    private final int verticalRadius;
    private final BiPredicate<LevelAccessor, BlockPos> condition;

    private MiniBiomeType(Builder builder) {
        this.id = builder.id;
        this.priority = builder.priority;
        this.threshold = builder.threshold;
        this.max = Math.max(builder.max, builder.threshold);
        this.contributions = new Object2IntOpenHashMap<>(builder.contributions);
        this.contributions.defaultReturnValue(0);
        this.provider = builder.provider;
        this.horizontalRadius = builder.horizontalRadius;
        this.verticalRadius = builder.verticalRadius;
        this.condition = builder.condition;
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(id);
    }

    public ResourceLocation id() {
        return id;
    }

    /// 数字小的优先。只在必须给出唯一答案时（{@link MiniBiome#primaryAt}）用来仲裁；
    /// 交错命中的多个迷你群系在 {@link MiniBiome#markersAt} 里都会返回。
    public int priority() {
        return priority;
    }

    public int threshold() {
        return threshold;
    }

    public int max() {
        return max;
    }

    public int horizontalRadius() {return horizontalRadius;}

    public int verticalRadius() {return verticalRadius;}

    public boolean usesBlocks() {return provider == null;}

    public boolean allows(LevelAccessor level, BlockPos pos) {return condition.test(level, pos);}

    /// 加权计数。方块来源用窗口求和，{@link Provider} 来源直接问它。
    public int count(LevelAccessor level, BlockPos pos, int[] windowCounts) {
        if (provider != null) return provider.count(level, pos);
        int total = 0;
        for (Object2IntMap.Entry<BlockCounters.Counter> entry : contributions.object2IntEntrySet()) {
            total += entry.getIntValue() * windowCounts[entry.getKey().index()];
        }
        return total;
    }

    public boolean matches(int count) {
        return count >= threshold;
    }

    /// 强度（渐变）：计数在 `[threshold, max]` 之间的归一化。
    public float influence(int count) {
        return MiniBiome.influence(count, threshold, max);
    }

    /// 非方块计数的来源（例如按 region 统计 NPC 数量的小镇）。
    @FunctionalInterface
    public interface Provider {
        int count(LevelAccessor level, BlockPos pos);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final Object2IntMap<BlockCounters.Counter> contributions = new Object2IntOpenHashMap<>();
        private @Nullable Provider provider;
        private int priority = 1000;
        private int threshold = 1;
        private int max = 1;
        private int horizontalRadius = MiniBiome.WINDOW_RADIUS;
        private int verticalRadius = MiniBiome.WINDOW_HALF_HEIGHT;
        private BiPredicate<LevelAccessor, BlockPos> condition = (level, pos) -> true;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        /// 查询半径，单位方块；局部计数以 4 格单元为精度，范围填写 4 的倍数。
        public Builder window(int horizontalRadius, int verticalRadius) {
            this.horizontalRadius = horizontalRadius;
            this.verticalRadius = verticalRadius;
            return this;
        }

        /// 附加成立条件，例如地下高度或结构条件；不影响其他同时成立的标记。
        public Builder condition(BiPredicate<LevelAccessor, BlockPos> condition) {
            this.condition = condition;
            return this;
        }

        /// @param max 强度达到 1.0 的计数；填 `threshold` 即为布尔化（强度只有 0/1）
        public Builder threshold(int threshold, int max) {
            this.threshold = threshold;
            this.max = max;
            return this;
        }

        /// 加一个正贡献的计数器
        public Builder count(BlockCounters.Counter counter, int weight) {
            counter.spatial();
            contributions.mergeInt(counter, weight, Integer::sum);
            return this;
        }

        /// 加一个抵消项（权重为负），例如向日葵抵消墓碑
        public Builder offset(BlockCounters.Counter counter, int weight) {
            return count(counter, -Math.abs(weight));
        }

        /// 改用非方块来源，此时 {@link #count} 注册的贡献会被忽略
        public Builder provider(Provider provider) {
            this.provider = provider;
            return this;
        }

        public MiniBiomeType build() {
            if (provider == null && contributions.isEmpty()) {
                throw new IllegalStateException("Mini biome " + id + " has neither block counters nor a provider");
            }
            return new MiniBiomeType(this);
        }
    }
}
