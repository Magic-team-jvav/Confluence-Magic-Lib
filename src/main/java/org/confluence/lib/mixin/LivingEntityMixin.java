package org.confluence.lib.mixin;

import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.mixed.LibLivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements LibLivingEntity {
    @Unique
    private int confluence$tickFreezeTime = 0;

    @Override
    public void confluence$setTickFreezeTime(int tick) {
        this.confluence$tickFreezeTime = tick;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tick(CallbackInfo ci) {
        if (confluence$tickFreezeTime > 0) {
            this.confluence$tickFreezeTime--;
            ci.cancel();
        }
    }
}
