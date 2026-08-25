package org.confluence.lib.integration.animation;

import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.constant.DataTickets;

public class PlayerAnimationState extends AnimationState<PlayerGeoAnimatable> {
    private float partialTick;

    public PlayerAnimationState(PlayerGeoAnimatable animatable) {
        super(animatable, 0, 0, 0, false);
    }

    public void update(float partialTick) {
        this.partialTick = partialTick;
        setData(DataTickets.TICK, (double) getAnimatable().player.tickCount + partialTick);
    }

    @Override
    public float getPartialTick() {
        return partialTick;
    }
}
