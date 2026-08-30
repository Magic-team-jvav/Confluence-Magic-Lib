package org.confluence.lib.mixin.integration.animation;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.integration.animation.ILibAbstractClientPlayer;
import org.confluence.lib.integration.animation.PlayerGeoModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;

@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {
    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 0))
    private void transformItem(
            CallbackInfo ci,
            @Local(argsOnly = true) LivingEntity living,
            @Local(argsOnly = true) PoseStack poseStack,
            @Local(argsOnly = true) HumanoidArm arm
    ) {
        if (living instanceof ILibAbstractClientPlayer player) {
            CoreGeoBone item = player.confluence$getAnimatable().model.getItem(arm);
            if (item != null) {
                PlayerGeoModel.applyItemBone(poseStack, item);
            }
        }
    }
}
