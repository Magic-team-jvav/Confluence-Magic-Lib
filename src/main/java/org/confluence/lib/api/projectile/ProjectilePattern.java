package org.confluence.lib.api.projectile;

import java.util.List;

/**
 * 将一次动作展开为一组待生成弹幕的纯布局接口。
 *
 * <p>实现可以创建并初始化实体，但绝对不能调用 {@code Level#addFreshEntity}。返回集合会由服务
 * 立即防御性复制并完整预检，只有整批通过后才会提交成本和世界生成。</p>
 */
@FunctionalInterface
public interface ProjectilePattern {
    /**
     * 创建当前动作的完整待发射布局。
     *
     * @return 尚未加入世界的弹幕描述
     */
    List<ProjectileLaunch> create(
            ProjectileFireContext context,
            ProjectileCombatSnapshot snapshot
    );
}
