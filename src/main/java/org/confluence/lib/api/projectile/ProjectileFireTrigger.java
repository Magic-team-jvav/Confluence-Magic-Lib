package org.confluence.lib.api.projectile;

/**
 * 由服务端武器动作识别的有限发射触发方式。
 *
 * <p>网络包只能传递这些固定枚举，不能携带动作 ID、伤害、速度、弹数、实体类型或目标坐标。
 * 原版弓与连弩入口应使用各自的释放触发，避免与普通攻击包重复生成弹幕。</p>
 */
public enum ProjectileFireTrigger {
    /**
     * 玩家按下攻击键产生的一次性动作，例如剑气或枪械射击。
     */
    ATTACK_PRESSED,
    /**
     * 玩家按下使用键产生的一次性动作，例如普通法杖施法。
     */
    USE_PRESSED,
    /**
     * 玩家保持使用状态时由服务端周期触发，例如受控持续法术。
     */
    CONTINUOUS_USE_TICK,
    /**
     * 近战动作进行中由服务端武器状态机产生的衍生弹幕 tick。
     */
    MELEE_ATTACK_TICK,
    /**
     * 玩家松开普通使用动作。
     */
    USE_RELEASED,
    /**
     * 原版弓完成蓄力后的权威释放入口。
     */
    VANILLA_BOW_RELEASE,
    /**
     * 原版连弩完成装填后的权威释放入口。
     */
    VANILLA_CROSSBOW_RELEASE
}
