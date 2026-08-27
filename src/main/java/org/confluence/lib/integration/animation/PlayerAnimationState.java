package org.confluence.lib.integration.animation;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.constant.DataTickets;

public class PlayerAnimationState extends AnimationState<PlayerGeoAnimatable> {
    private float limbSwing;
    private float limbSwingAmount;
    private boolean isMoving;
    private float partialTick;

    public boolean isAttacking;

    public ModelPropertiesCallback headCallback = PlayerAnimationState::allStack;
    public ModelPropertiesCallback torsoCallback = PlayerAnimationState::scaleStack;
    public ModelPropertiesCallback rightArmCallback = PlayerAnimationState::scaleStack;
    public ModelPropertiesCallback leftArmCallback = PlayerAnimationState::scaleStack;
    public ModelPropertiesCallback rightLegCallback = this::legStack;
    public ModelPropertiesCallback leftLegCallback = this::legStack;
    public ModelPropertiesCallback capeCallback = PlayerAnimationState::allStack;
    public ModelPropertiesCallback bodyCallback = PlayerAnimationState::allStack;

    public static void allStack(ModelPart part, GeoBone bone) {
        ModelPropertiesCallback.pos(part, bone, true);
        ModelPropertiesCallback.rot(part, bone, true);
        ModelPropertiesCallback.scale(part, bone, true);
    }

    public static void scaleStack(ModelPart part, GeoBone bone) {
        ModelPropertiesCallback.pos(part, bone, false);
        ModelPropertiesCallback.rot(part, bone, false);
        ModelPropertiesCallback.scale(part, bone, true);
    }

    public void legStack(ModelPart part, GeoBone bone) {
        ModelPropertiesCallback.pos(part, bone, true);
        ModelPropertiesCallback.scale(part, bone, true);
        if (!isMoving()) {
            ModelPropertiesCallback.rot(part, bone, false);
        }
    }

    public PlayerAnimationState(PlayerGeoAnimatable animatable) {
        super(animatable, 0, 0, 0, false);
    }

    public void update(float partialTick) {
        this.partialTick = partialTick;
        this.isMoving = false;
        AbstractClientPlayer player = getAnimatable().player;
        Vec3 velocity = player.getDeltaMovement();
        if ((Math.abs(velocity.x) + Math.abs(velocity.z)) * 0.5 > 0) {
            boolean shouldSit = player.isPassenger() && player.getVehicle() != null;
            if (!shouldSit && player.isAlive()) {
                this.limbSwingAmount = Math.min(1, player.walkAnimation.speed(partialTick));
                this.limbSwing = player.walkAnimation.position(partialTick);
                if (player.isBaby()) {
                    this.limbSwing *= 3f;
                }
                if (limbSwingAmount != 0) {
                    this.isMoving = true;
                }
            }
        }
        setData(DataTickets.TICK, (double) player.tickCount + partialTick);
    }

    @Override
    public float getPartialTick() {
        return partialTick;
    }

    @Override
    public boolean isMoving() {
        return isMoving;
    }

    @Override
    public float getLimbSwing() {
        return limbSwing;
    }

    @Override
    public float getLimbSwingAmount() {
        return limbSwingAmount;
    }

    @FunctionalInterface
    public interface ModelPropertiesCallback {
        void update(ModelPart part, GeoBone bone);

        static void pos(ModelPart part, GeoBone bone, boolean stack) {
            if (stack) {
                part.x += bone.getPosX();
                part.y -= bone.getPosY();
                part.z += bone.getPosZ();
            } else {
                PartPose ip = part.getInitialPose();
                part.x = ip.x + bone.getPosX();
                part.y = ip.y - bone.getPosY();
                part.z = ip.z + bone.getPosZ();
            }
        }

        static void rot(ModelPart part, GeoBone bone, boolean stack) {
            if (stack) {
                part.xRot -= bone.getRotX();
                part.yRot -= bone.getRotY();
                part.zRot += bone.getRotZ();
            } else {
                PartPose ip = part.getInitialPose();
                part.xRot = ip.xRot - bone.getRotX();
                part.yRot = ip.yRot - bone.getRotY();
                part.zRot = ip.zRot + bone.getRotZ();
            }
        }

        static void scale(ModelPart part, GeoBone bone, boolean stack) {
            if (stack) {
                part.xScale *= bone.getScaleX();
                part.yScale *= bone.getScaleY();
                part.zScale *= bone.getScaleZ();
            } else {
                part.xScale = bone.getScaleX();
                part.yScale = bone.getScaleY();
                part.zScale = bone.getScaleZ();
            }
        }
    }
}
