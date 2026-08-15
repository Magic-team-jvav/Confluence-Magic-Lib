package org.confluence.lib.api.projectile;

import org.jetbrains.annotations.Nullable;

/**
 * 由能够发射玩家战斗弹幕的物品实现的动作提供接口。
 *
 * <p>该接口不规定物品继承树，因此剑、弓、GeckoLib 物品和附属模组物品都可直接接入。
 * 返回的动作只能依据服务端上下文构建；返回 {@code null} 表示当前状态没有动作。</p>
 */
public interface ProjectileWeaponAction {
    /**
     * 根据当前服务端手持物与触发状态创建一次不可变动作声明。
     *
     * @return 当前状态可执行的动作；没有动作时返回 {@code null}
     */
    @Nullable ProjectileFireAction createProjectileFireAction(ProjectileFireContext context);
}
