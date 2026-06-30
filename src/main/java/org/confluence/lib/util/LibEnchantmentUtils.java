package org.confluence.lib.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class LibEnchantmentUtils {
    public static int getEnchantmentLevel(Enchantment enchantment, @Nullable ItemStack stack) {
        if (stack != null) {
            return EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
        }
        return 0;
    }

    public static ItemStack enchantedBook(Enchantment enchantment, int level) {
        ItemStack book = Items.ENCHANTED_BOOK.getDefaultInstance();
        book.enchant(enchantment, level);
        return book;
    }

    public static void runIterationOnHand(ServerPlayer player, Consumer<ItemStack> consumer) {
        consumer.accept(player.getMainHandItem());
        consumer.accept(player.getOffhandItem());
    }

    public static class SlotGroups {
        public static final EquipmentSlot[] ARMOR_N_MAINHAND = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
        public static final EquipmentSlot[] MAINHAND = new EquipmentSlot[]{EquipmentSlot.MAINHAND};
        public static final EquipmentSlot[] ANY = EquipmentSlot.values();
        public static final EquipmentSlot[] ARMOR = new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    }
}
