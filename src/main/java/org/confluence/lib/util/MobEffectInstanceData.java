package org.confluence.lib.util;

import com.mojang.serialization.Codec;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.function.Supplier;

public record MobEffectInstanceData(
        Supplier<MobEffect> effect,
        int duration,
        int amplifier,
        boolean ambient,
        boolean visible,
        boolean showIcon
) {
    public static final Codec<MobEffectInstanceData> CODEC = MobEffectInstance.CODEC.xmap(MobEffectInstanceData::instance2Entry, MobEffectInstanceData::entry2Instance);

    public MobEffectInstanceData(Supplier<MobEffect> effect, int duration) {
        this(effect, duration, 0);
    }

    public MobEffectInstanceData(Supplier<MobEffect> effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, true);
    }

    public MobEffectInstanceData(Supplier<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible) {
        this(effect, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstanceData(MobEffect effect, int duration) {
        this(effect, duration, 0);
    }

    public MobEffectInstanceData(MobEffect effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, true);
    }

    public MobEffectInstanceData(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible) {
        this(effect, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstanceData(MobEffect effect, int duration, int amplifier, boolean ambient, boolean visible, boolean showIcon) {
        this(() -> effect, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstance create() {
        return entry2Instance(this);
    }

    public static MobEffectInstanceData instance2Entry(MobEffectInstance instance) {
        return new MobEffectInstanceData(
                instance.getEffect(),
                instance.getDuration(),
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
        );
    }

    public static MobEffectInstance entry2Instance(MobEffectInstanceData effect) {
        return new MobEffectInstance(
                effect.effect.get(),
                effect.duration,
                effect.amplifier,
                effect.ambient,
                effect.visible,
                effect.showIcon
        );
    }
}
