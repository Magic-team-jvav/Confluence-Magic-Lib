package org.confluence.lib.mixed;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.api.event.ProcessCriticalDamageEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.event.PortEventHandler;

public interface ILibDamageSource {
    void confluence$setCritical(boolean critical);

    boolean confluence$isCritical();

    @ApiStatus.Internal
    static float processCritical(@Nullable Entity attacker, float amount, LivingEntity victim, DamageSource damageSource) {
        if (of(damageSource).confluence$isCritical()) return amount; // 只有原先不是暴击才继续暴击逻辑
        boolean crit = false;
        /// [LibAttributes#applyToArrow]
        if (damageSource.getDirectEntity() instanceof AbstractArrow arrow) {
            crit |= arrow.isCritArrow();
        }
        // 检查完箭矢暴击后不再检查暴击
        if (!crit && attacker instanceof Player player && !LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE) &&
                LibMathUtils.checkChance(player.getAttributeValue(ConfluenceMagicLib.CRITICAL_CHANCE), player.getRandom1211())
        ) {
            player.crit(victim);
            crit = true;
        }
        ProcessCriticalDamageEvent event = PortEventHandler.postEventWithReturn(new ProcessCriticalDamageEvent(victim, damageSource, amount, crit));
        of(damageSource).confluence$setCritical(event.isCritical());
        amount = event.getAmount();
        if (event.isCritical()) {
            amount *= event.getCriticalDamageMultiplier();
        }
        return amount;
    }

    static ILibDamageSource of(DamageSource damageSource) {
        return (ILibDamageSource) damageSource;
    }
}
