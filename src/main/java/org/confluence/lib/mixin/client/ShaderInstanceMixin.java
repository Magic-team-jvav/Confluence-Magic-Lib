package org.confluence.lib.mixin.client;

import net.minecraft.client.renderer.ShaderInstance;
import org.confluence.lib.client.light.DynamicLightGpu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
    @Unique
    private DynamicLightGpu.Binding confluence$lightBinding;

    @Inject(method = "apply", at = @At("TAIL"))
    private void applyDynamicLight(CallbackInfo ci) {
        if (confluence$lightBinding == null)
            confluence$lightBinding = new DynamicLightGpu.Binding((ShaderInstance) (Object) this);
        confluence$lightBinding.apply();
    }

    @Inject(method = {"clear", "close"}, at = @At("HEAD"))
    private void clearDynamicLight(CallbackInfo ci) {
        if (confluence$lightBinding != null) confluence$lightBinding.clear();
    }
}
