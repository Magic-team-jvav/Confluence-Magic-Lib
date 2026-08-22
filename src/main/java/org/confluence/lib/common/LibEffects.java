package org.confluence.lib.common;

import PortLib.extensions.net.minecraftforge.registries.DeferredRegister.PortDeferredRegisterExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.effect.GravitationEffect;
import org.confluence.lib.common.effect.HoneyEffect;
import org.confluence.lib.common.effect.PublicMobEffect;
import org.confluence.lib.util.LibEntityUtils;
import org.mesdag.portlib.wrapper.world.entity.ai.attributes.PortAttributeModifier;

import java.util.Comparator;

public final class LibEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ConfluenceMagicLib.LIB_ID);

    public static final RegistryObject<MobEffect> CONFUSED = EFFECTS.register("confused", () -> new PublicMobEffect(MobEffectCategory.HARMFUL, 0x8B008B));
    public static final RegistryObject<MobEffect> GRAVITATION = EFFECTS.register("gravitation", GravitationEffect::new);
    public static final RegistryObject<MobEffect> PALADINS_SHIELD = EFFECTS.register("paladins_shield", () -> new PublicMobEffect(MobEffectCategory.BENEFICIAL, 0x666666));
    public static final RegistryObject<MobEffect> CEREBRAL_MINDTRICK = PortDeferredRegisterExtension.register(EFFECTS, "cerebral_mindtrick", id -> new PublicMobEffect(MobEffectCategory.NEUTRAL, 0xFFA885).addAttributeModifier(LibAttributes.getCriticalChance(), id, 0.04, PortAttributeModifier.Operation.ADD_VALUE));
    public static final RegistryObject<MobEffect> HONEY = EFFECTS.register("honey", HoneyEffect::new);

    public static void healPerSecond(LivingEntity living, float amount) {
        if (living.level().getGameTime() % 20L == 0) {
            living.heal(amount);
        }
    }

    public static float applyPaladinsShield(LivingEntity victim, DamageSource damageSource, float amount) {
        if (victim instanceof ServerPlayer sp && !isPaladinsShieldOwner(sp)) {
            MutableFloat atomic = new MutableFloat(amount);
            Object team = LibEntityUtils.getTeam(sp);
            sp.level().players().stream().filter(player -> player != sp && // player不是自己
                    player != damageSource.getEntity() && // player不是给自己造成过伤害的
                    LibEntityUtils.getTeam(player) == team && // player的队伍与自己的相同
                    player.getHealth() / player.getMaxHealth() > 0.25F && // player血量大于最大血量的25%
                    isPaladinsShieldOwner(player) && // player拥有圣骑士盾
                    player.distanceToSqr(sp) < 1024.0 // player与自己的距离在32米内
            ).min(Comparator.comparingDouble(player -> player.distanceToSqr(sp))).ifPresent(player -> {
                float damage = amount * 0.25F;
                player.hurt(victim.damageSources().playerAttack(sp), damage);
                atomic.subtract(damage);
            });
            return atomic.getValue();
        }
        return amount;
    }

    public static boolean isPaladinsShieldOwner(LivingEntity living) {
        MobEffectInstance effect = living.getEffect(PALADINS_SHIELD.get());
        return effect != null && effect.getAmplifier() != 0;
    }
}
