package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.fluids.FluidType;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements SelfGetter<LocalPlayer> {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 1))
    private boolean skipSlowdown(boolean original) {
        if (!original && confluence$self().getUseItem().is(LibTags.Items.SKIP_USING_SLOWDOWN)) {
            return true;
        }
        return original;
    }

    /// 反重力时下潜键向水面上方施力。
    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sinkInFluid(Lnet/neoforged/neoforge/fluids/FluidType;)V", remap = false))
    private boolean sinkUpFluid(LocalPlayer instance, FluidType fluidType) {
        if (GravitationHandler.isShouldRot()) {
            instance.jumpInFluid(fluidType);
            return false;
        }
        return true;
    }

    /// 创造飞行的竖直输入跟随当前重力方向。
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float flip(float original) {
        return original * GravitationHandler.getJumpDir();
    }
}
