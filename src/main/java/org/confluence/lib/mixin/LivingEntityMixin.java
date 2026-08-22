package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.ILibLivingEntity;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ILibLivingEntity {
    @Shadow
    public abstract boolean hasEffect(MobEffect effect);

    @Shadow
    public abstract Map<MobEffect, MobEffectInstance> getActiveEffectsMap();

    @ModifyArg(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"), index = 1)
    private float armorPenetration(float totalArmor, @Local(argsOnly = true) DamageSource damageSource) {
        return LibAttributes.applyArmorPenetration(confluence$self(), damageSource, totalArmor);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 confused(Vec3 vec3) {
        if (hasEffect(LibEffects.CONFUSED.get())) {
            return vec3.reverse();
        }
        return vec3;
    }

    @ModifyArg(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"), index = 2)
    private double modifyParticlePosY(double posY) {
        ILibEntity self = ILibEntity.of(confluence$self());
        if (self.confluence$isShouldRot()) {
            return posY + self.confluence$getDimensionHeight() - 0.15;
        }
        return posY;
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 reversed(Vec3 vec3) {
        if (ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            return new Vec3(-vec3.x, vec3.y, vec3.z);
        }
        return vec3;
    }

    @WrapMethod(method = "hasEffect")
    private boolean hasEffect(MobEffect effect, Operation<Boolean> original) {
        return ILibLivingEntity.hasEffect(getActiveEffectsMap(), effect);
    }

    @WrapMethod(method = "getEffect")
    private MobEffectInstance getEffect(MobEffect effect, Operation<MobEffectInstance> original) {
        return ILibLivingEntity.getEffect(getActiveEffectsMap(), effect);
    }

    @WrapWithCondition(method = "onEffectUpdated", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;addAttributeModifiers(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/ai/attributes/AttributeMap;I)V"))
    private boolean shouldAdd(MobEffect instance, LivingEntity livingEntity, AttributeMap attributeMap, int amplifier, @Local(argsOnly = true) MobEffectInstance effectInstance) {
        return ILibMobEffectInstance.of(effectInstance).confluence$isEnabled();
    }
}
