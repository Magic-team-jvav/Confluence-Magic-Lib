package org.confluence.lib.mixin.client;

import net.minecraft.world.entity.Entity;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class ClientEntityMixin implements SelfGetter<Entity> {
    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
    private void eyeHeight(CallbackInfoReturnable<Float> cir) {
        ILibEntity self = ILibEntity.of(confluence$self());
        if (self.confluence$isShouldRot()) {
            cir.setReturnValue(self.confluence$getDimensionHeight() * 0.15F);
        }
    }
}
