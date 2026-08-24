package org.confluence.lib.integration.animation;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.model.GeoModel;

public class PlayerGeoModel extends GeoModel<PlayerGeoAnimatable> {
    @Override
    public ResourceLocation getModelResource(PlayerGeoAnimatable animatable) {
        if (animatable.currentGroup == null) {
            throw new UnsupportedOperationException();
        }
        return animatable.currentGroup.model();
    }

    @Override
    public ResourceLocation getTextureResource(PlayerGeoAnimatable animatable) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResourceLocation getAnimationResource(PlayerGeoAnimatable animatable) {
        if (animatable.currentGroup == null) {
            throw new UnsupportedOperationException();
        }
        return animatable.currentGroup.animation();
    }

    public void reset(PlayerModel<AbstractClientPlayer> vanillaModel) {
        vanillaModel.head.resetPose();
        vanillaModel.hat.resetPose();
        vanillaModel.body.resetPose();
        vanillaModel.jacket.resetPose();
        vanillaModel.rightArm.resetPose();
        vanillaModel.rightSleeve.resetPose();
        vanillaModel.leftArm.resetPose();
        vanillaModel.leftSleeve.resetPose();
        vanillaModel.rightLeg.resetPose();
        vanillaModel.rightPants.resetPose();
        vanillaModel.leftLeg.resetPose();
        vanillaModel.leftPants.resetPose();
    }

    public void update(PlayerModel<AbstractClientPlayer> vanillaModel) {
        AnimationProcessor<PlayerGeoAnimatable> processor = getAnimationProcessor();
        updateProperties(vanillaModel.head, vanillaModel.hat, processor.getBone("head"));
        // body The whole player
        updateProperties(vanillaModel.body, vanillaModel.jacket, processor.getBone("torso"));
        updateProperties(vanillaModel.rightArm, vanillaModel.rightSleeve, processor.getBone("right_arm"));
        updateProperties(vanillaModel.leftArm, vanillaModel.leftSleeve, processor.getBone("left_arm"));
        updateProperties(vanillaModel.rightLeg, vanillaModel.rightPants, processor.getBone("right_leg"));
        updateProperties(vanillaModel.leftLeg, vanillaModel.leftPants, processor.getBone("left_leg"));
        // right item
        // left item
//        updateProperties(vanillaModel.cloak, processor.getBone("cape"));
        // elytra
    }

    protected void updateProperties(ModelPart inner, ModelPart layer, @Nullable CoreGeoBone bone) {
        if (bone != null) {
            updateProperties(inner, bone);
            updateProperties(layer, bone);
        }
    }

    protected void updateProperties(ModelPart part, CoreGeoBone bone) {
        part.x += bone.getPosX();
        part.y -= bone.getPosY();
        part.z += bone.getPosZ();
        part.xRot -= bone.getRotX();
        part.yRot -= bone.getRotY();
        part.zRot += bone.getRotZ();
        part.xScale *= bone.getScaleX();
        part.yScale *= bone.getScaleY();
        part.zScale *= bone.getScaleZ();
    }
}
