package org.confluence.lib.common;

import PortLib.extensions.net.minecraftforge.registries.DeferredRegister.PortDeferredRegisterExtension;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.effect.GravitationEffect;
import org.confluence.lib.common.effect.HoneyEffect;
import org.confluence.lib.common.effect.PublicMobEffect;
import org.mesdag.portlib.wrapper.world.entity.ai.attributes.PortAttributeModifier;

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
}
