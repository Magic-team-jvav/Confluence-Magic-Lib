package org.confluence.lib.integration.animation;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.Vec3;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public class PlayerAnimationState extends AnimationState<PlayerGeoAnimatable> {
    private float limbSwing;
    private float limbSwingAmount;
    private boolean isMoving;
    private float partialTick;

    public boolean isAttacking;

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
}
