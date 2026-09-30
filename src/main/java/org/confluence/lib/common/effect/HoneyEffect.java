package org.confluence.lib.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.util.LibEntityUtils;

/// 蜂蜜效果：每 10 tick 回 0.1 点血。
///
/// **1.20 侧的两处差异**：
/// 1. 1.21 的 TerraCurio 版本把「玩家」判据写成 `IEntity.of(living).terra_curio$isPlayer()`，
///    那是 TerraCurio 自己的 mixed 接口 —— 搬进 Magic-Lib 后不能反向依赖 TerraCurio
///    （TerraCurio 依赖 Magic-Lib，反向引用会成环）。1.20 的 Lib 版本用的是
///    `living instanceof Player`，此处照 1.20。
/// 2. 1.20 的 `applyEffectTick` 是 `void` + `isDurationEffectTick`；1.21.1 的前者返回 `boolean`
///    （是否真的施加了效果）、后者改名 `shouldApplyEffectTickThisTick`
///    （1.21.1 `MobEffect.java:64/74`），逻辑不变。
public class HoneyEffect extends MobEffect {
    public HoneyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFF00);
    }

    @Override
    public boolean applyEffectTick(LivingEntity living, int amplifier) {
        living.heal(0.1F);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    public static void applyHoneyEffect(LivingEntity living) {
        if (LibEntityUtils.isAnimal(living) || living instanceof Player) {
            MobEffectInstance effect = living.getEffect(LibEffects.HONEY);
            if (effect == null || effect.getDuration() < 220) {
                living.addEffect(new MobEffectInstance(LibEffects.HONEY, 600));
            }
        } else if (living instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
    }
}
