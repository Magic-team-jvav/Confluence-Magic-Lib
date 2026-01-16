package org.confluence.lib.util;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

public record MobEffectInstanceData(Holder<MobEffect> effect, int duration, int amplifier) {
    public MobEffectInstance create() {
        return new MobEffectInstance(effect, duration, amplifier);
    }
}
