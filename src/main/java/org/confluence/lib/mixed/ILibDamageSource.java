package org.confluence.lib.mixed;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.api.event.ProcessCriticalDamageEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface ILibDamageSource {
    void confluence$setCritical(boolean critical);

    boolean confluence$isCritical();

    @ApiStatus.Internal
    MutableBoolean WARNED = new MutableBoolean();

    @ApiStatus.Internal
    static float processCritical(@Nullable Entity attacker, float amount, LivingEntity victim, DamageSource damageSource) {
        boolean crit = false;
        /// [LibAttributes#applyToArrow]
        if (damageSource.getDirectEntity() instanceof AbstractArrow arrow) {
            crit |= arrow.isCritArrow();
        }
        // 检查完箭矢暴击后不再检查暴击
        if (!crit && attacker instanceof Player player && !LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE) &&
                LibMathUtils.checkChance(player.getAttributeValue(ConfluenceMagicLib.CRITICAL_CHANCE), player.getRandom())
        ) {
            player.crit(victim);
            crit = true;
        }
        ProcessCriticalDamageEvent event;
        if (damageSource instanceof ILibDamageSource cds) {
            crit |= cds.confluence$isCritical();
            event = NeoForge.EVENT_BUS.post(new ProcessCriticalDamageEvent(victim, damageSource, amount, crit));
            cds.confluence$setCritical(event.isCritical());
        } else {
            event = NeoForge.EVENT_BUS.post(new ProcessCriticalDamageEvent(victim, damageSource, amount, crit));
            if (WARNED.isFalse()) {
                WARNED.setTrue();
                ConfluenceMagicLib.LOGGER.warn("DamageSource had remodified by unknown mod, so critical damage indicator expired now");
            }
        }
        amount = event.getAmount();
        if (event.isCritical()) {
            amount *= event.getCriticalDamageMultiplier();
        }
        return amount;
    }
}
