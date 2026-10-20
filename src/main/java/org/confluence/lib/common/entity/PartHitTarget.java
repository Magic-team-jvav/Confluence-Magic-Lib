package org.confluence.lib.common.entity;

import net.minecraft.world.entity.Entity;

/// 部位自身接收伤害；另行声明阵营归属和穿透去重身份。
public interface PartHitTarget {
    /// 用于阵营和索敌归属，独立血量部位仍可归属同一本体。
    Entity encounterOwner();

    /// 共享血量的部位返回本体，独立血量的部位返回自身。
    Entity dedupeIdentity();

    /// 部位失效或破坏后不再参与直接命中。
    boolean acceptsDirectHit();
}
