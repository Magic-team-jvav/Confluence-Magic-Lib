package org.confluence.lib.common.effect;

import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.confluence.lib.ConfluenceMagicLib;

public class GravitationEffect extends MobEffect {
    public static final ResourceLocation ID = ConfluenceMagicLib.asResource("gravitation_flip");
    public static final ImmutableMultimap<Holder<Attribute>, AttributeModifier> GRAVITY = ImmutableMultimap.of(
            Attributes.GRAVITY, new AttributeModifier(ID, -2.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
    );

    public GravitationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAA00AA);
    }
}
