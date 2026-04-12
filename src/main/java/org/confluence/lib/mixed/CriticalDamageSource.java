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

    @ApiStatus.Internal
    static float processCritical(@Nullable Entity attacker, float amount, LivingEntity victim, DamageSource damageSource) {
        boolean crit = false;
        /// [LibAttributes#applyToArrow]
        if (damageSource.getDirectEntity() instanceof AbstractArrow arrow) {
            crit |= arrow.isCritArrow();
        }
        // 检查完箭矢暴击后不再检查暴击
        if (!crit && attacker instanceof Player player &&
                !LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE) &&
                LibMathUtils.checkChance(player.getAttributeValue(ConfluenceMagicLib.CRITICAL_CHANCE), player.getRandom())
        ) {
            player.crit(victim);
            crit = true;
        }
        if (crit) { // 暴击伤害统一乘1.5倍
            amount *= 1.5F;
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
