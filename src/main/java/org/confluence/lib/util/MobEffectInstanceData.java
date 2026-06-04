package org.confluence.lib.util;

import PortLib.extensions.net.minecraft.world.effect.MobEffectInstance.PortMobEffectInstanceExtension;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import org.mesdag.portlib.wrapper.world.effect.MobEffectHolder;

public record MobEffectInstanceData(
        Holder<MobEffect> effect,
        int duration,
        int amplifier,
        boolean ambient,
        boolean visible,
        boolean showIcon
) {
    public static final Codec<MobEffectInstanceData> CODEC = PortMobEffectInstanceExtension.codec().xmap(MobEffectInstanceData::instance2Entry, MobEffectInstanceData::entry2Instance);

    public MobEffectInstanceData(Holder<MobEffect> effect, int duration) {
        this(effect, duration, 0);
    }

    public MobEffectInstanceData(Holder<MobEffect> effect, int duration, int amplifier) {
        this(effect, duration, amplifier, false, true);
    }

    public MobEffectInstanceData(Holder<MobEffect> effect, int duration, int amplifier, boolean ambient, boolean visible) {
        this(effect, duration, amplifier, ambient, visible, visible);
    }

    public MobEffectInstance create() {
        return entry2Instance(this);
    }

    public static MobEffectInstanceData instance2Entry(MobEffectInstance instance) {
        return new MobEffectInstanceData(
                MobEffectHolder.wrap(instance.getEffect()),
                instance.getDuration(),
                instance.getAmplifier(),
                instance.isAmbient(),
                instance.isVisible(),
                instance.showIcon()
        );
    }

    public static MobEffectInstance entry2Instance(MobEffectInstanceData effect) {
        return new MobEffectInstance(
                effect.effect.value(),
                effect.duration,
                effect.amplifier,
                effect.ambient,
                effect.visible,
                effect.showIcon
        );
    }
}
