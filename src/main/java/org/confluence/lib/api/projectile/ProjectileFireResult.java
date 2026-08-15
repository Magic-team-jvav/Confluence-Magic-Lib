package org.confluence.lib.api.projectile;

/**
 * 服务端发射流程的精确终止结果，供网络入口、测试与附属模组诊断。
 */
public enum ProjectileFireResult {
    /**
     * 全部弹幕已加入世界，资源、冷却和成功表现均已提交。
     */
    SUCCESS,
    /**
     * 玩家死亡、被移除、处于旁观模式或不在有效服务端世界。
     */
    PLAYER_UNAVAILABLE,
    /**
     * 当前手持物没有提供弹幕动作。
     */
    NO_ACTION,
    /**
     * 当前动作不接受请求中的固定触发方式。
     */
    UNSUPPORTED_TRIGGER,
    /**
     * 同一玩家、手和触发方式在同一服务端 tick 已处理过一次。
     */
    DUPLICATE_REQUEST,
    /**
     * 当前服务端手持物仍处于冷却。
     */
    COOLDOWN,
    /**
     * 动作自己的服务端校验拒绝执行。
     */
    VALIDATION_REJECTED,
    /**
     * 成本无法为本次动作提供足够资源。
     */
    NO_RESOURCE,
    /**
     * 动作没有生成任何有效待发射弹幕。
     */
    NO_VALID_PROJECTILE,
    /**
     * 准备阶段内玩家已经更换、修改当前手持武器。
     */
    WEAPON_CHANGED,
    /**
     * 动作返回空值、非有限数值、越界集合或其他违反公共契约的数据。
     */
    INVALID_ACTION_RESULT,
    /**
     * 成本提交抛出异常；发射服务已尝试执行幂等补偿。
     */
    COST_COMMIT_FAILED,
    /**
     * 世界生成阶段失败；已加入实体、资源和本次发射添加的冷却均已回滚。
     */
    SPAWN_ROLLED_BACK,
    /**
     * 附属提供的回滚动作抛出异常，服务无法宣称资源已被精确恢复。
     */
    ROLLBACK_FAILED;

    /**
     * 返回该结果是否代表完整成功。
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
