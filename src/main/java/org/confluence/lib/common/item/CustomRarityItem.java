package org.confluence.lib.common.item;

import PortLib.extensions.net.minecraft.world.item.Item.PortItemExtension;
import PortLib.extensions.net.minecraft.world.item.ItemStack.PortItemStackExtension;
import com.google.common.collect.Multimap;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.component.ModRarity;
import org.mesdag.portlib.wrapper.world.item.component.PortItemAttributeModifiers;

import java.util.function.Consumer;

public class CustomRarityItem extends Item {
    protected PortItemAttributeModifiers modifiers;

    public CustomRarityItem(Properties properties) {
        this(properties, ModRarity.GRAY);
    }

    public CustomRarityItem(ModRarity rarity) {
        this(new Properties(), rarity);
    }

    public CustomRarityItem(Properties properties, ModRarity rarity) {
        super(PortItemExtension.Properties.component(properties, ConfluenceMagicLib.MOD_RARITY, rarity));
    }

    public CustomRarityItem addAttributeModifiers(Consumer<PortItemAttributeModifiers.PortBuilder> consumer) {
        PortItemAttributeModifiers.PortBuilder builder = PortItemAttributeModifiers.builder();
        consumer.accept(builder);
        this.modifiers = builder.build();
        return this;
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        return modifiers == null ? super.getAttributeModifiers(slot, stack) : PortItemStackExtension.getPortAttributeModifiers(stack).getAttributeModifiers(slot);
    }
}
