package org.confluence.lib.common.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.util.LibEntityUtils;

public class HoneyEffect extends MobEffect {
    public HoneyEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xFFFF00);
    }

    @Override
    public void applyEffectTick(LivingEntity living, int amplifier) {
        living.heal(0.1F);
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % 10 == 0;
    }

    public static void applyHoneyEffect(LivingEntity living) {
        if (LibEntityUtils.isAnimal(living) || living instanceof Player) {
            MobEffectInstance effect = living.getEffect(LibEffects.HONEY.get());
            if (effect == null || effect.getDuration() < 220) {
                living.addEffect(new MobEffectInstance(LibEffects.HONEY.get(), 600));
            }
        } else if (living instanceof AbstractPiglin piglin) {
            piglin.setImmuneToZombification(true);
        }
    }
}
