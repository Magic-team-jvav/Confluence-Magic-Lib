package org.confluence.lib.mixin.client;

import net.minecraft.client.renderer.GameRenderer;
import org.confluence.lib.client.light.DynamicLightGpu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void endDynamicLight(CallbackInfo ci) {
        DynamicLightGpu.end();
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void closeDynamicLight(CallbackInfo ci) {
        DynamicLightGpu.close();
    }
}
