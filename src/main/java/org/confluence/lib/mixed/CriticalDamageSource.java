package org.confluence.lib.mixed;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public interface CriticalDamageSource {
    void confluence$setCritical(boolean critical);

    boolean confluence$isCritical();

    @ApiStatus.Internal
    MutableBoolean WARNED = new MutableBoolean();

    static float processCritical(@Nullable Entity attacker, float amount, LivingEntity victim, DamageSource damageSource) {
        boolean crit = false;
        if (attacker instanceof Player player && !LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE)) {
            if (LibMathUtils.checkChance(player.getAttributeValue(ConfluenceMagicLib.CRITICAL_CHANCE), player.getRandom())) {
                amount *= 1.5F;
                player.crit(victim);
                crit = true;
            }
        }
        if (damageSource.getDirectEntity() instanceof AbstractArrow arrow) {
            crit |= arrow.isCritArrow();
        }
        if (!(damageSource instanceof CriticalDamageSource iDamageSource)) {
            if (WARNED.isFalse()) {
                WARNED.setTrue();
                ConfluenceMagicLib.LOGGER.warn("DamageSource had remodified by unknown mod, so critical damage indicator expired now");
            }
            return amount;
        }
        crit |= iDamageSource.confluence$isCritical();
        iDamageSource.confluence$setCritical(crit);
        return amount;
    }
}
