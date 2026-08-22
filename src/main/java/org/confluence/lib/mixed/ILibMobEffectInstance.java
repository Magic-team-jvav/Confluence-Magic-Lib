package org.confluence.lib.mixed;

import net.minecraft.world.effect.MobEffectInstance;

public interface ILibMobEffectInstance {
    void confluence$setEnabled(boolean enabled);

    boolean confluence$isEnabled();

    static ILibMobEffectInstance of(MobEffectInstance instance) {
        return (ILibMobEffectInstance) instance;
    }
}
