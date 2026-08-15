package org.confluence.lib.mixed;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.api.event.ProcessCriticalDamageEvent;
import org.confluence.lib.api.projectile.ProjectileCombatSnapshot;
import org.confluence.lib.api.projectile.ProjectileCombatSnapshotCarrier;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.event.PortEventHandler;

public interface ILibDamageSource {
    void confluence$setCritical(boolean critical);

    boolean confluence$isCritical();

    /**
     * 将攻击创建时冻结的战斗快照直接附加到本次伤害来源；传入 {@code null} 可清除快照。
     */
    void confluence$setCombatSnapshot(@Nullable ProjectileCombatSnapshot snapshot);

    /**
     * 返回本次伤害来源携带的战斗快照；未附加时返回 {@code null}。
     */
    @Nullable ProjectileCombatSnapshot confluence$getCombatSnapshot();

    @ApiStatus.Internal
    MutableBoolean WARNED = new MutableBoolean();

    @ApiStatus.Internal
    static float processCritical(@Nullable Entity attacker, float amount, LivingEntity victim, DamageSource damageSource) {
        return processCritical(
                attacker,
                amount,
                victim,
                damageSource,
                ProjectileCombatSnapshotCarrier.find(damageSource)
        );
    }

    /**
     * 处理快照或旧伤害源的暴击阶段。
     *
     * <p>快照存在时，发射阶段的三态判定是唯一基础结果：已确定不暴击时不得按命中时属性重掷，
     * 未解析状态也只交给暴击事件扩展。没有快照时完整保留旧箭矢与玩家暴击率逻辑。</p>
     */
    @ApiStatus.Internal
    static float processCritical(
            @Nullable Entity attacker,
            float amount,
            LivingEntity victim,
            DamageSource damageSource,
            @Nullable ProjectileCombatSnapshot snapshot
    ) {
        boolean crit = false;
        if (snapshot == null) {
            // 旧箭矢在生成阶段可能已写入暴击标记，优先沿用该结果。
            if (damageSource.getDirectEntity() instanceof AbstractArrow arrow) {
                crit = arrow.isCritArrow();
            }
            // 箭矢尚未暴击时，旧伤害链仍按命中时的玩家暴击率抽取一次。
            if (!crit && attacker instanceof Player player &&
                    !LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE) &&
                    LibMathUtils.checkChance(
                            player.getAttributeValue(ConfluenceMagicLib.CRITICAL_CHANCE),
                            player.getRandom()
                    )
            ) {
                player.crit(victim);
                crit = true;
            }
        } else {
            // UNRESOLVED 与 NON_CRITICAL 都从 false 开始，后续扩展只能通过事件明确改写。
            crit = snapshot.criticalResolution().isCritical();
        }
        ProcessCriticalDamageEvent event;
        ILibDamageSource lds = of(damageSource);
        if (lds == null) {
            event = PortEventHandler.postEventWithReturn(new ProcessCriticalDamageEvent(victim, damageSource, amount, crit));
            if (WARNED.isFalse()) {
                WARNED.setTrue();
                ConfluenceMagicLib.LOGGER.warn("DamageSource had remodified by unknown mod, so critical damage indicator expired now");
            }
        } else {
            // 旧伤害源兼容既有混入标记；快照则必须以发射时结果为准，避免读取过期状态。
            if (snapshot == null) crit |= lds.confluence$isCritical();
            event = PortEventHandler.postEventWithReturn(new ProcessCriticalDamageEvent(victim, damageSource, amount, crit));
            lds.confluence$setCritical(event.isCritical());
        }
        amount = event.getAmount();
        if (event.isCritical()) {
            amount *= event.getCriticalDamageMultiplier();
        }
        return amount;
    }

    static @Nullable ILibDamageSource of(DamageSource damageSource) {
        if (damageSource instanceof ILibDamageSource lds) {
            return lds;
        }
        return null;
    }
}
