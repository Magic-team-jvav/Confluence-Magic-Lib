package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.fluids.FluidType;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements SelfGetter<LocalPlayer> {
    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isPassenger()Z", ordinal = 0))
    private boolean skipSlowdown(boolean original) {
        if (!original && confluence$self().getUseItem().is(LibTags.Items.SKIP_USING_SLOWDOWN)) {
            return true;
        }
        return original;
    }
    @WrapWithCondition(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;sinkInFluid(Lnet/minecraftforge/fluids/FluidType;)V"), remap = false)
    private boolean sinkUpFluid(LocalPlayer instance, FluidType fluidType) {
        if (GravitationHandler.isShouldRot()) {
            instance.jumpInFluid(fluidType);
            return false;
        }
        return true;
    }

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Abilities;getFlyingSpeed()F"))
    private float flip(float original) {
        return original * GravitationHandler.getJumpDir();
    }
}
