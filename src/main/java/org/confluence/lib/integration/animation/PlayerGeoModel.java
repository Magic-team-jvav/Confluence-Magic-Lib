package org.confluence.lib.integration.animation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.HumanoidArm;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationProcessor;
import software.bernie.geckolib.model.GeoModel;

public class PlayerGeoModel extends GeoModel<PlayerGeoAnimatable> {
    private boolean updated;
    private @Nullable CoreGeoBone rightItem;
    private @Nullable CoreGeoBone leftItem;

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

    public @Nullable CoreGeoBone getItem(HumanoidArm arm) {
        return arm == HumanoidArm.RIGHT ? rightItem : leftItem;
    }

    public void reset(PlayerModel<AbstractClientPlayer> vanillaModel) {
        if (updated) {
            this.leftItem = null;
            this.rightItem = null;
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
            this.updated = false;
        }
    }

    // todo 鞘翅 (elytra)
    public void update(PlayerAnimationState state, PlayerModel<AbstractClientPlayer> model) {
        AnimationProcessor<PlayerGeoAnimatable> processor = getAnimationProcessor();
        CoreGeoBone head = processor.getBone("head");
        if (head != null) {
            state.headCallback.update(model.head, head);
            state.headCallback.update(model.hat, head);
        }
        CoreGeoBone torso = processor.getBone("torso");
        if (torso != null) {
            state.torsoCallback.update(model.body, torso);
            state.torsoCallback.update(model.jacket, torso);
        }
        CoreGeoBone rightArm = processor.getBone("right_arm");
        if (rightArm != null) {
            state.rightArmCallback.update(model.rightArm, rightArm);
            state.rightArmCallback.update(model.rightSleeve, rightArm);
        }
        CoreGeoBone leftArm = processor.getBone("left_arm");
        if (leftArm != null) {
            state.leftArmCallback.update(model.leftArm, leftArm);
            state.leftArmCallback.update(model.leftSleeve, leftArm);
        }
        CoreGeoBone rightLeg = processor.getBone("right_leg");
        if (rightLeg != null) {
            state.rightLegCallback.update(model.rightLeg, rightLeg);
            state.rightLegCallback.update(model.rightPants, rightLeg);
        }
        CoreGeoBone leftLeg = processor.getBone("left_leg");
        if (leftLeg != null) {
            state.leftLegCallback.update(model.leftLeg, leftLeg);
            state.leftLegCallback.update(model.leftPants, leftLeg);
        }
        // right item / left item
        // 物品骨骼无法直接作用到原版 ModelPart 上，这里仅暂存，
        // 由 PlayerModel#translateToHand 的 mixin 在渲染手持物品时叠加其位姿
        this.rightItem = processor.getBone("right_item");
        this.leftItem = processor.getBone("left_item");
        CoreGeoBone cape = processor.getBone("cape");
        if (cape != null) {
            state.capeCallback.update(model.cloak, cape);
        }
        // elytra
        CoreGeoBone body = processor.getBone("body");
        if (body != null) {
            state.bodyCallback.update(model.head, body);
            state.bodyCallback.update(model.hat, body);
            state.bodyCallback.update(model.body, body);
            state.bodyCallback.update(model.jacket, body);
            state.bodyCallback.update(model.rightArm, body);
            state.bodyCallback.update(model.rightSleeve, body);
            state.bodyCallback.update(model.leftArm, body);
            state.bodyCallback.update(model.leftSleeve, body);
            state.bodyCallback.update(model.rightLeg, body);
            state.bodyCallback.update(model.rightPants, body);
            state.bodyCallback.update(model.leftLeg, body);
            state.bodyCallback.update(model.leftPants, body);
        }
        this.updated = true;
    }

    public static void applyItemBone(PoseStack poseStack, CoreGeoBone bone) {
        CoreGeoBone parent = bone.getParent();
        float pivotX;
        float pivotY;
        float pivotZ;
        if (parent == null) {
            pivotX = 0;
            pivotY = 0;
            pivotZ = 0;
        } else {
            pivotX = parent.getPivotX() - bone.getPivotX();
            pivotY = -(bone.getPivotY() - parent.getPivotY());
            pivotZ = bone.getPivotZ() - parent.getPivotZ();
        }

        poseStack.translate(bone.getPosX() / 16.0F, -bone.getPosY() / 16.0F, bone.getPosZ() / 16.0F);
        poseStack.translate(pivotX / 16.0F, pivotY / 16.0F, pivotZ / 16.0F);
        if (bone.getRotX() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotation(-bone.getRotX()));
        }
        if (bone.getRotY() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotation(-bone.getRotY()));
        }
        if (bone.getRotZ() != 0.0F) {
            poseStack.mulPose(Axis.ZP.rotation(bone.getRotZ()));
        }
        poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
        poseStack.translate(-pivotX / 16.0F, -pivotY / 16.0F, -pivotZ / 16.0F);
    }
}
