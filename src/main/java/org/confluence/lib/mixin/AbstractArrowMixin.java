package org.confluence.lib.mixin;

import net.minecraft.world.entity.projectile.AbstractArrow;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements SelfGetter<AbstractArrow> {
    @ModifyVariable(method = "onHitEntity", at = @At(value = "STORE", ordinal = 0), name = "d0")
    private double modify(double d0) {
        return LibAttributes.applyArrowKnockback(confluence$self().getOwner(), d0);
    }
}
