package org.confluence.lib.mixin;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements SelfGetter<AbstractArrow> {
    @ModifyVariable(method = "doKnockback", at = @At(value = "STORE", ordinal = 0), name = "knockback")
    private double modify(double knockback) {
        return LibAttributes.applyArrowKnockback(confluence$self().getOwner(), knockback);
    }
}
