package org.confluence.lib.mixin.integration.animation;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.integration.animation.ILibAbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("HEAD"))
    private void reset(CallbackInfo ci, @Local(argsOnly = true) LivingEntity living) {
        if (living instanceof ILibAbstractClientPlayer player) {
            player.confluence$getAnimatable().reset();
        }
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void update(CallbackInfo ci, @Local(argsOnly = true) LivingEntity living) {
        if (living instanceof ILibAbstractClientPlayer player) {
            player.confluence$getAnimatable().handleAnimations(Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false));
        }
    }
}
