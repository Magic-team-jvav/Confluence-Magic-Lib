package org.confluence.lib.api.permanent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.confluence.lib.ConfluenceMagicLib;

import java.util.Objects;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * 一个可注册、可恢复的永久强化定义。
 *
 * <p>默认等级保存在 {@link PermanentUpgradeData} 中。附属模组如果已有自己的玩家数据，
 * 可以通过 {@link Builder#levelAccess(ToIntFunction, ObjIntConsumer)} 接入，而不必复制物品使用逻辑。
 * 效果回调必须是幂等的，因为玩家登录、重生和数据恢复时会再次执行。</p>
 */
public final class PermanentUpgrade {
    private final ResourceLocation id;
    private final int minimumLevel;
    private final int maxLevel;
    private final LevelAccess levelAccess;
    private final Requirement requirement;
    private final Effect effect;
    private final boolean projectAtZero;

    private PermanentUpgrade(Builder builder) {
        this.id = builder.id;
        this.minimumLevel = builder.minimumLevel;
        this.maxLevel = builder.maxLevel;
        this.levelAccess = builder.levelAccess;
        this.requirement = builder.requirement;
        this.effect = builder.effect;
        this.projectAtZero = builder.projectAtZero;
    }

    /**
     * 返回存档中使用的稳定标识。
     */
    public ResourceLocation id() {
        return id;
    }

    /**
     * 返回正向强化可达到的最高等级。
     */
    public int maxLevel() {
        return maxLevel;
    }

    /**
     * 返回反向强化可达到的最低等级，默认为零。
     */
    public int minimumLevel() {
        return minimumLevel;
    }

    /**
     * 从定义使用的数据源读取当前等级。
     */
    public int getLevel(ServerPlayer player) {
        return levelAccess.getLevel(player);
    }

    /**
     * 尝试增加一级。
     */
    public PermanentUpgradeResult tryApply(ServerPlayer player) {
        return tryChange(player, 1);
    }

    /**
     * 按给定增量修改等级。正数用于强化，负数用于回溯类物品。
     *
     * <p>自定义等级写入器应当一次性完成写入，并在写入前失败。API 不再对写入结果进行二次读取猜测，
     * 这样实现方只需要遵守清晰的 setter 契约。</p>
     */
    public PermanentUpgradeResult tryChange(ServerPlayer player, int levelDelta) {
        Objects.requireNonNull(player, "player");
        if (levelDelta == 0) throw new IllegalArgumentException("levelDelta must not be zero");

        final int previous;
        try {
            previous = getLevel(player);
        } catch (RuntimeException exception) {
            logFailure("read", player, exception);
            return PermanentUpgradeResult.EFFECT_REJECTED;
        }

        long requested = (long) previous + levelDelta;
        if (requested > maxLevel) return PermanentUpgradeResult.ALREADY_MAXIMUM;
        if (requested < minimumLevel) return PermanentUpgradeResult.ALREADY_MINIMUM;

        int target = (int) requested;
        PermanentUpgradeContext context = new PermanentUpgradeContext(player, previous, target, false);
        final PermanentUpgradeResult requirementResult;
        try {
            requirementResult = Objects.requireNonNull(requirement.test(context),
                    "Permanent upgrade requirement returned null");
        } catch (RuntimeException exception) {
            logFailure("check requirement for", player, exception);
            return PermanentUpgradeResult.EFFECT_REJECTED;
        }
        if (!requirementResult.isApplied()) return requirementResult;

        try {
            levelAccess.setLevel(player, target);
        } catch (RuntimeException exception) {
            logFailure("write", player, exception);
            return PermanentUpgradeResult.EFFECT_REJECTED;
        }

        applyEffect(new PermanentUpgradeContext(player, previous, target, false));
        return PermanentUpgradeResult.APPLIED;
    }

    private void applyEffect(PermanentUpgradeContext context) {
        try {
            effect.apply(context);
        } catch (RuntimeException exception) {
            // 等级已经写入权威数据。保留状态并在下一次恢复生命周期重新投影效果。
            logFailure("apply effect for", context.player(), exception);
        }
    }

    private void logFailure(String operation, ServerPlayer player, RuntimeException exception) {
        ConfluenceMagicLib.LOGGER.error("Failed to {} permanent upgrade {} for {}", operation, id,
                player.getGameProfile().getName(), exception);
    }

    /**
     * 按当前等级重新安装效果，供登录、重生和外部数据恢复时调用。
     */
    public boolean reconcile(ServerPlayer player) {
        try {
            int storedLevel = getLevel(player);
            int level = Math.max(minimumLevel, Math.min(storedLevel, maxLevel));
            if (storedLevel != level) levelAccess.setLevel(player, level);
            if (level == 0 && !projectAtZero) return true;
            effect.apply(new PermanentUpgradeContext(player, level, level, true));
            return true;
        } catch (RuntimeException exception) {
            ConfluenceMagicLib.LOGGER.error("Failed to reconcile permanent upgrade {} for {}", id,
                    player.getGameProfile().getName(), exception);
            return false;
        }
    }

    /**
     * 创建默认使用 MagicLib 玩家数据的永久强化定义。
     */
    public static Builder builder(ResourceLocation id, int maxLevel) {
        return new Builder(id, maxLevel);
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final int maxLevel;
        private int minimumLevel;
        private LevelAccess levelAccess;
        private Requirement requirement = context -> PermanentUpgradeResult.APPLIED;
        private Effect effect = context -> {};
        private boolean projectAtZero;

        private Builder(ResourceLocation id, int maxLevel) {
            this.id = Objects.requireNonNull(id, "id");
            if (maxLevel <= 0) throw new IllegalArgumentException("maxLevel must be positive");
            this.maxLevel = maxLevel;
            this.levelAccess = LevelAccess.defaultData(id);
        }

        /**
         * 设置回溯类物品可以达到的最低等级。
         */
        public Builder minimumLevel(int minimumLevel) {
            if (minimumLevel >= maxLevel) {
                throw new IllegalArgumentException("minimumLevel must be lower than maxLevel");
            }
            this.minimumLevel = minimumLevel;
            return this;
        }

        /**
         * 设置写入等级前执行的资格检查。
         */
        public Builder requirement(Requirement requirement) {
            this.requirement = Objects.requireNonNull(requirement, "requirement");
            return this;
        }

        /**
         * 设置等级提交后的幂等效果投影。
         */
        public Builder effect(Effect effect) {
            this.effect = Objects.requireNonNull(effect, "effect");
            return this;
        }

        /**
         * 要求恢复阶段在零级时也执行效果，用于移除旧修饰符或刷新同步。
         */
        public Builder projectAtZero() {
            this.projectAtZero = true;
            return this;
        }

        /**
         * 接入附属模组自己的玩家持久数据。
         */
        public Builder levelAccess(ToIntFunction<ServerPlayer> getter, ObjIntConsumer<ServerPlayer> setter) {
            this.levelAccess = new LevelAccess(getter, setter);
            return this;
        }

        public PermanentUpgrade build() {
            return new PermanentUpgrade(this);
        }
    }

    /**
     * 外部玩家数据与永久强化定义之间的最小读写契约。
     */
    public record LevelAccess(ToIntFunction<ServerPlayer> getter,
                              ObjIntConsumer<ServerPlayer> setter) {
        public LevelAccess {
            Objects.requireNonNull(getter, "getter");
            Objects.requireNonNull(setter, "setter");
        }

        int getLevel(ServerPlayer player) {
            return getter.applyAsInt(player);
        }

        void setLevel(ServerPlayer player, int level) {
            setter.accept(player, level);
        }

        static LevelAccess defaultData(ResourceLocation id) {
            return new LevelAccess(player -> PermanentUpgradeData.of(player).getLevel(id),
                    (player, level) -> PermanentUpgradeData.of(player).setLevel(id, level));
        }
    }

    /**
     * 写入状态前的资格检查，不应在这里修改永久状态。
     */
    @FunctionalInterface
    public interface Requirement {
        PermanentUpgradeResult test(PermanentUpgradeContext context);
    }

    /**
     * 把已提交等级投影到属性、能力或同步状态。
     */
    @FunctionalInterface
    public interface Effect {
        void apply(PermanentUpgradeContext context);
    }
}
