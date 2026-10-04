package org.confluence.lib.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class LibEnchantmentUtils {
    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantments, @Nullable ItemStack stack) {
        if(stack == null || stack.isEmpty()) return 0;
        // 从物品堆栈中获取附魔信息，如果没有则使用空的附魔集合。
        ItemEnchantments itemenchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        for (Object2IntMap.Entry<Holder<Enchantment>> entry : itemenchantments.entrySet()) { // 获得物品的所有附魔
            if (entry.getKey().is(enchantments)) { // key是附魔，value是附魔的等级
                return entry.getIntValue();
            }
        }

        // 没有附魔则返回0。
        return 0;
    }

    public static ItemStack enchantedBook(HolderLookup.RegistryLookup<Enchantment> registryLookup, ResourceKey<Enchantment> key, int level) {
        ItemStack book = Items.ENCHANTED_BOOK.getDefaultInstance();
        book.enchant(registryLookup.getOrThrow(key), level);
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
