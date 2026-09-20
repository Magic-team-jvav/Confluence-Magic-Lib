package org.confluence.lib.api.animation.first_person;

import software.bernie.geckolib.core.animation.Animation;
import software.bernie.geckolib.core.animation.RawAnimation;

public record HandAnimationClip(String animation, Animation.LoopType loopType) {
    public HandAnimationClip {
        if (animation == null || animation.isBlank()) {
            throw new IllegalArgumentException("Animation name cannot be blank");
        }
    }

    public static HandAnimationClip playOnce(String animation) {
        return new HandAnimationClip(animation, Animation.LoopType.PLAY_ONCE);
    }

    public static HandAnimationClip loop(String animation) {
        return new HandAnimationClip(animation, Animation.LoopType.LOOP);
    }

    public RawAnimation rawAnimation() {
        return RawAnimation.begin().then(animation, loopType);
    }
}
