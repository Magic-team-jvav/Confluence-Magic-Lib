package org.confluence.lib.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.effect.GravitationEffect;
import org.confluence.lib.common.effect.HoneyEffect;
import org.confluence.lib.common.effect.PublicMobEffect;
import org.confluence.lib.util.LibEntityUtils;

import java.util.Comparator;

/// Magic-Lib 自有的效果注册表。
///
/// **归属**：这 5 个效果在 1.20.1 的分叉后架构里由 Magic-Lib 持有
/// （`Confluence-Magic-Lib/.../lib/common/LibEffects.java`），TerraCurio 里已经没有 `TCEffects`，
/// 且 TerraCurio 自己反过来引用本类（如 `PaladinsShield`、`TCUtils`）。
/// 1.21 侧原先把它们放在 `org.confluence.terra_curio.common.init.TCEffects`（`terra_curio` 命名空间），
/// 现按同一归属迁到这里（`confluence_magic_lib` 命名空间）；
/// 旧 id 的兼容见 {@link ConfluenceMagicLib} 构造函数里的 `addAlias`。
public final class LibEffects {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, ConfluenceMagicLib.LIB_ID);

    public static final DeferredHolder<MobEffect, MobEffect> CONFUSED = EFFECTS.register("confused", () -> new PublicMobEffect(MobEffectCategory.HARMFUL, 0x8B008B));
    public static final DeferredHolder<MobEffect, MobEffect> GRAVITATION = EFFECTS.register("gravitation", GravitationEffect::new);
    public static final DeferredHolder<MobEffect, MobEffect> PALADINS_SHIELD = EFFECTS.register("paladins_shield", () -> new PublicMobEffect(MobEffectCategory.BENEFICIAL, 0x666666));
    /// 1.20 侧这一条走的是 PortLib 的 `PortDeferredRegisterExtension.register(EFFECTS, name, Function)`，
    /// 目的是拿到效果自己的 id 来建属性修饰符；1.21.1 的 `DeferredRegister#register(String, Function)`
    /// 原生就是这个签名，直接照 `TCEffects` 的写法即可。
    public static final DeferredHolder<MobEffect, MobEffect> CEREBRAL_MINDTRICK = EFFECTS.register("cerebral_mindtrick", id -> new PublicMobEffect(MobEffectCategory.NEUTRAL, 0xFFA885).addAttributeModifier(LibAttributes.getCriticalChance(), id, 0.04, AttributeModifier.Operation.ADD_VALUE));
    public static final DeferredHolder<MobEffect, MobEffect> HONEY = EFFECTS.register("honey", HoneyEffect::new);

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
        MobEffectInstance effect = living.getEffect(PALADINS_SHIELD);
        return effect != null && effect.getAmplifier() != 0;
    }
}
