package org.confluence.lib.common.effect;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.effect.MobEffectCategory;
import org.mesdag.portlib.wrapper.world.effect.PortMobEffect;

public class PublicMobEffect extends PortMobEffect {
    public PublicMobEffect(MobEffectCategory category, int color, ParticleOptions particle) {
        super(category, color, particle);
    }

    public PublicMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
