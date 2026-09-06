package org.confluence.lib.common.effect;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.mesdag.portlib.diff.Diff;

import java.util.UUID;

public class GravitationEffect extends MobEffect {
    public static final UUID ID = UUID.nameUUIDFromBytes("gravitation_flip".getBytes());
    @Diff
    public static final Multimap<Attribute, AttributeModifier> GRAVITY = ImmutableMultimap.of(
            Attributes.GRAVITY.value(), new AttributeModifier(ID, "gravitation_flip", -2.0, AttributeModifier.Operation.MULTIPLY_TOTAL)
    );

    public GravitationEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xAA00AA);
    }
}
