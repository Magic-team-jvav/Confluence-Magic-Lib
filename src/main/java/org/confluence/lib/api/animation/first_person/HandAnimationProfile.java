package org.confluence.lib.api.animation.first_person;

import java.util.ArrayList;
import java.util.List;

public record HandAnimationProfile(List<HandAnimationChannel> channels) {
    public HandAnimationProfile(Builder channels) {
        this(validateAndCopy(channels));
    }

    private static List<HandAnimationChannel> validateAndCopy(Builder channels) {
        if (channels.channels.isEmpty()) {
            throw new IllegalArgumentException("An animation profile needs at least one channel");
        }
        return channels.channels;
    }

    public boolean isAnimation(HandAnimationAction action, String animationName) {
        if (animationName == null) {
            return false;
        }
        return channels.stream().flatMap(channel -> channel.clip(action).stream()).anyMatch(clip -> clip.animation().equals(animationName));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<HandAnimationChannel> channels = new ArrayList<>();

        public Builder channel(HandAnimationChannel channel) {
            if (channels.stream().anyMatch(existing -> existing.name().equals(channel.name()))) {
                throw new IllegalArgumentException("Duplicate animation channel: " + channel.name());
            }
            channels.add(channel);
            return this;
        }

        public HandAnimationProfile build() {
            return new HandAnimationProfile(this);
        }
    }
}
