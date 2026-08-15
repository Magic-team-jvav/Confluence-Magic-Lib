package org.confluence.lib.api.projectile;

/**
 * 玩家武器弹幕的主伤害通道。
 *
 * <p>一个战斗快照只能选择一个主通道。实体是否带有原版 projectile 标签仍可供减伤、
 * 免疫和其他模组识别，但不能据此让一个弹幕同时叠加远程与魔法伤害。</p>
 */
public enum ProjectileDamageChannel {
    /**
     * 剑气、近战武器附带弹幕和长矛衍生弹幕。
     */
    MELEE,
    /**
     * 枪械、弓和连弩弹幕。
     */
    RANGED,
    /**
     * 法杖、魔力枪和其他魔法武器弹幕。
     */
    MAGIC,
    /**
     * 鞭子、召唤物和哨兵产生的攻击。
     */
    SUMMON
}
