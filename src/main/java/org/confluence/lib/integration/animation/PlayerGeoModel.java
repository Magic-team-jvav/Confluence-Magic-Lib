package org.confluence.lib.integration.animation;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationProcessor;
import software.bernie.geckolib.cache.object.GeoBone;
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
        GeoBone body = processor.getBone("body");
        if (body != null) {
            updateProperties(vanillaModel.head, body);
            updateProperties(vanillaModel.hat, body);
            updateProperties(vanillaModel.body, body);
            updateProperties(vanillaModel.jacket, body);
            updateProperties(vanillaModel.rightArm, body);
            updateProperties(vanillaModel.rightSleeve, body);
            updateProperties(vanillaModel.leftArm, body);
            updateProperties(vanillaModel.leftSleeve, body);
            updateProperties(vanillaModel.rightLeg, body);
            updateProperties(vanillaModel.rightPants, body);
            updateProperties(vanillaModel.leftLeg, body);
            updateProperties(vanillaModel.leftPants, body);
        }
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

    protected void updateProperties(ModelPart inner, ModelPart layer, @Nullable GeoBone bone) {
        if (bone != null) {
            updateProperties(inner, bone);
            updateProperties(layer, bone);
        }
    }

    protected void updateProperties(ModelPart part, GeoBone bone) {
        // 是否叠加
        // 是否与原版的进行叠加，也可以与其他通过修改原版模型的一起生效
        boolean isStacking = true;
        // 混合权重 0-1
        // 0~1的时候会逐渐混合，1的时候会完全替换
        float w = 1;
        if (isStacking) {
            part.x += bone.getPosX();
            part.y -= bone.getPosY();
            part.z += bone.getPosZ();
            part.xRot -= bone.getRotX();
            part.yRot -= bone.getRotY();
            part.zRot += bone.getRotZ();
            part.xScale *= bone.getScaleX();
            part.yScale *= bone.getScaleY();
            part.zScale *= bone.getScaleZ();
        } else {
            PartPose ip = part.getInitialPose();
            part.x += (ip.x + bone.getPosX() - part.x) * w;
            part.y += (ip.y + -bone.getPosY() - part.y) * w;
            part.z += (ip.z + bone.getPosZ() - part.z) * w;
            part.xRot += (ip.xRot + -bone.getRotX() - part.xRot) * w;
            part.yRot += (ip.yRot + -bone.getRotY() - part.yRot) * w;
            part.zRot += (ip.zRot + bone.getRotZ() - part.zRot) * w;
            part.xScale *= (1f + bone.getScaleX() - part.xScale) * w;
            part.yScale *= (1f + bone.getScaleY() - part.yScale) * w;
            part.zScale *= (1f + bone.getScaleZ() - part.zScale) * w;
        }
    }
}
