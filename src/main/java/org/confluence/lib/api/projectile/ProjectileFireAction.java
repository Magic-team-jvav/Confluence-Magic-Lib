package org.confluence.lib.api.projectile;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 一次玩家武器发射动作的不可变服务端声明。
 *
 * <p>动作只描述战斗基础值、触发方式、成本、布局、冷却与成功回调；具体弹药、魔力、声音和
 * 泰拉瑞亚武器规则由 Otherworld 或附属模组组合。构建阶段会拒绝所有非有限或越界数值。</p>
 */
public final class ProjectileFireAction {
    private final ProjectileDamageChannel damageChannel;
    private final float baseDamage;
    private final float baseVelocity;
    private final float baseKnockback;
    private final boolean inherentCritical;
    private final float criticalChanceBonus;
    private final Set<ProjectileFireTrigger> triggers;
    private final ProjectileCost cost;
    private final ProjectilePattern pattern;
    private final int cooldownTicks;
    private final Predicate<ProjectileFireContext> validator;
    private final Consumer<ProjectileFireContext> successAction;

    private ProjectileFireAction(Builder builder) {
        this.damageChannel = builder.damageChannel;
        this.baseDamage = requireNonNegative(builder.baseDamage, "Base damage");
        this.baseVelocity = requirePositive(builder.baseVelocity, "Base velocity");
        this.baseKnockback = requireNonNegative(builder.baseKnockback, "Base knockback");
        this.inherentCritical = builder.inherentCritical;
        this.criticalChanceBonus = requireNonNegative(builder.criticalChanceBonus, "Critical chance bonus");
        this.triggers = Set.copyOf(builder.triggers);
        if (triggers.isEmpty())
            throw new IllegalArgumentException("Projectile fire triggers must not be empty");
        this.cost = builder.cost;
        this.pattern = builder.pattern;
        if (builder.cooldownTicks < 0)
            throw new IllegalArgumentException("Cooldown ticks must be non-negative");
        this.cooldownTicks = builder.cooldownTicks;
        this.validator = builder.validator;
        this.successAction = builder.successAction;
    }

    /**
     * 创建具有指定主通道、组合成本与纯布局的动作构建器。
     */
    public static Builder builder(
            ProjectileDamageChannel damageChannel,
            ProjectileCost cost,
            ProjectilePattern pattern
    ) {
        return new Builder(damageChannel, cost, pattern);
    }

    /**
     * 返回唯一主伤害通道。
     */
    public ProjectileDamageChannel damageChannel() {
        return damageChannel;
    }

    /**
     * 返回应用主通道倍率前的动作基础伤害。
     */
    public float baseDamage() {
        return baseDamage;
    }

    /**
     * 返回应用通道弹速规则前的动作基础速度。
     */
    public float baseVelocity() {
        return baseVelocity;
    }

    /**
     * 返回应用攻击击退属性前的动作基础击退。
     */
    public float baseKnockback() {
        return baseKnockback;
    }

    /**
     * 返回原版或具体武器是否已经确定本次为固有暴击。
     */
    public boolean inherentCritical() {
        return inherentCritical;
    }

    /**
     * 返回武器动作本身提供的额外暴击率。
     *
     * <p>该值与发射者暴击属性在同一次快照判定中相加，适合枪械数据事件、附属武器配置等
     * 无法直接表示为物品属性修饰符的请求局部数值。</p>
     */
    public float criticalChanceBonus() {
        return criticalChanceBonus;
    }

    /**
     * 返回动作是否接受给定的有限服务端触发方式。
     */
    public boolean supports(ProjectileFireTrigger trigger) {
        return triggers.contains(trigger);
    }

    /**
     * 返回将在实体预构建前准备、世界生成前提交的组合成本。
     */
    public ProjectileCost cost() {
        return cost;
    }

    /**
     * 返回不得直接修改世界的纯弹幕布局。
     */
    public ProjectilePattern pattern() {
        return pattern;
    }

    /**
     * 返回成功生成整批弹幕后提交的物品冷却 tick 数。
     */
    public int cooldownTicks() {
        return cooldownTicks;
    }

    boolean validate(ProjectileFireContext context) {
        return validator.test(context);
    }

    void runSuccessAction(ProjectileFireContext context) {
        successAction.accept(context);
    }

    /**
     * 不可变发射动作的构建器。
     */
    public static final class Builder {
        private final ProjectileDamageChannel damageChannel;
        private final ProjectileCost cost;
        private final ProjectilePattern pattern;
        private float baseDamage;
        private float baseVelocity = 1.0F;
        private float baseKnockback;
        private boolean inherentCritical;
        private float criticalChanceBonus;
        private final EnumSet<ProjectileFireTrigger> triggers =
                EnumSet.of(ProjectileFireTrigger.ATTACK_PRESSED);
        private int cooldownTicks;
        private Predicate<ProjectileFireContext> validator = context -> true;
        private Consumer<ProjectileFireContext> successAction = context -> {};

        private Builder(
                ProjectileDamageChannel damageChannel,
                ProjectileCost cost,
                ProjectilePattern pattern
        ) {
            this.damageChannel = Objects.requireNonNull(damageChannel, "Damage channel must not be null");
            this.cost = Objects.requireNonNull(cost, "Projectile cost must not be null");
            this.pattern = Objects.requireNonNull(pattern, "Projectile pattern must not be null");
        }

        /**
         * 设置应用唯一主通道倍率前的基础伤害。
         */
        public Builder baseDamage(float value) {
            this.baseDamage = value;
            return this;
        }

        /**
         * 设置应用远程弹速属性前的基础速度。
         */
        public Builder baseVelocity(float value) {
            this.baseVelocity = value;
            return this;
        }

        /**
         * 设置应用攻击击退属性前的基础击退。
         */
        public Builder baseKnockback(float value) {
            this.baseKnockback = value;
            return this;
        }

        /**
         * 设置具体武器或原版流程已经确定的固有暴击结果。
         */
        public Builder inherentCritical(boolean value) {
            this.inherentCritical = value;
            return this;
        }

        /**
         * 设置动作额外暴击率。
         *
         * <p>构建时要求值为非负有限数；一通常表示百分之百。该值不会替代玩家属性，
         * 而会由快照解析器在发射时与玩家暴击率统一裁定。</p>
         */
        public Builder criticalChanceBonus(float value) {
            this.criticalChanceBonus = value;
            return this;
        }

        /**
         * 用一个非空触发集合替换默认的按下攻击触发。
         */
        public Builder triggers(ProjectileFireTrigger first, ProjectileFireTrigger... remaining) {
            Objects.requireNonNull(first, "First projectile fire trigger must not be null");
            Objects.requireNonNull(remaining, "Projectile fire triggers must not be null");
            this.triggers.clear();
            this.triggers.add(first);
            Arrays.stream(remaining).forEach(trigger -> this.triggers.add(
                    Objects.requireNonNull(trigger, "Projectile fire trigger must not be null")));
            return this;
        }

        /**
         * 设置成功发射后施加的非负冷却 tick 数。
         */
        public Builder cooldownTicks(int value) {
            this.cooldownTicks = value;
            return this;
        }

        /**
         * 设置成本准备前运行的服务端动作校验。
         */
        public Builder validator(Predicate<ProjectileFireContext> value) {
            this.validator = Objects.requireNonNull(value, "Projectile fire validator must not be null");
            return this;
        }

        /**
         * 注册只在整批实体成功生成后执行的声音、统计或其他表现回调。
         */
        public Builder successAction(Consumer<ProjectileFireContext> value) {
            this.successAction = Objects.requireNonNull(value, "Projectile success action must not be null");
            return this;
        }

        /**
         * 校验全部声明并创建不可变动作。
         */
        public ProjectileFireAction build() {
            return new ProjectileFireAction(this);
        }
    }

    private static float requireNonNegative(float value, String fieldName) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static float requirePositive(float value, String fieldName) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
        return value;
    }
}
