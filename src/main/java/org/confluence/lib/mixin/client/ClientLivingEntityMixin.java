package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin implements SelfGetter<LivingEntity> {
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void fall(CallbackInfo ci, @Local(argsOnly = true) double motionY) {
        if (motionY > 0.0 && ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            confluence$self().fallDistance += (float) motionY;
        }
    }
}
