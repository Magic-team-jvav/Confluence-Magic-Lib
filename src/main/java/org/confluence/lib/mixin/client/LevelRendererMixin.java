package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.confluence.lib.client.DynamicLightDispatcher;
import org.confluence.lib.client.light.DynamicLightGpu;
import org.confluence.lib.mixed.SelfGetter;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 899)
public class LevelRendererMixin implements SelfGetter<LevelRenderer> {
    @ModifyReturnValue(method = "getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I", at = @At("RETURN"))
    private static int enhanceLightColor(int original, BlockAndTintGetter level, BlockState state, BlockPos pos) {
        if (DynamicLightGpu.terrainGpu()) return original;
        return DynamicLightDispatcher.getDynamicLight(level, state, pos, original);
    }

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void renderLevel(DeltaTracker deltaTracker, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightTexture lightTexture, Matrix4f frustumMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        DynamicLightDispatcher.INSTANCE.beginFrame(confluence$self(), camera, frustumMatrix, deltaTracker.getGameTimeDeltaPartialTick(true));
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void clearDynamicLight(ClientLevel level, CallbackInfo ci) {
        DynamicLightDispatcher.setLevel(level);
    }
}
