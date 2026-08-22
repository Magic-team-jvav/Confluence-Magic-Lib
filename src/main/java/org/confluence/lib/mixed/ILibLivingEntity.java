package org.confluence.lib.mixed;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface ILibLivingEntity extends SelfGetter<LivingEntity> {
    static boolean hasEffect(Map<MobEffect, MobEffectInstance> activeEffects, MobEffect effect) {
        MobEffectInstance instance = activeEffects.get(effect);
        return instance != null && ILibMobEffectInstance.of(instance).confluence$isEnabled();
    }

    static @Nullable MobEffectInstance getEffect(Map<MobEffect, MobEffectInstance> activeEffects, MobEffect effect) {
        MobEffectInstance instance = activeEffects.get(effect);
        return instance == null || !ILibMobEffectInstance.of(instance).confluence$isEnabled() ? null : instance;
    }

    static ILibLivingEntity of(LivingEntity living) {
        return (ILibLivingEntity) living;
    }
}
