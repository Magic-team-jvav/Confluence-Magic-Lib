package org.confluence.lib.api.projectile;

import net.minecraft.world.damagesource.DamageSource;
import org.confluence.lib.mixed.ILibDamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * 由需要参加 MagicLib 快照伤害链的弹幕实体实现。
 *
 * <p>设置操作只应在服务端创建或读取实体时发生；快照本身不可变，因此命中阶段可以安全共享。</p>
 */
public interface ProjectileCombatSnapshotCarrier {
    /**
     * 返回当前战斗快照；尚未初始化或数据损坏时返回 {@code null}。
     */
    @Nullable ProjectileCombatSnapshot getProjectileCombatSnapshot();

    /**
     * 安装发射时或当前格式 NBT 恢复出的战斗快照。
     */
    void setProjectileCombatSnapshot(ProjectileCombatSnapshot snapshot);

    /**
     * 优先读取伤害来源直接携带的快照，其次读取直接实体携带的快照，不读取间接攻击者的当前装备。
     */
    static @Nullable ProjectileCombatSnapshot find(DamageSource source) {
        if (source == null) {
            return null;
        }
        ILibDamageSource damageSource = ILibDamageSource.of(source);
        if (damageSource != null && damageSource.confluence$getCombatSnapshot() != null) {
            return damageSource.confluence$getCombatSnapshot();
        }
        if (source.getDirectEntity() instanceof ProjectileCombatSnapshotCarrier carrier) {
            return carrier.getProjectileCombatSnapshot();
        }
        return null;
    }
}
