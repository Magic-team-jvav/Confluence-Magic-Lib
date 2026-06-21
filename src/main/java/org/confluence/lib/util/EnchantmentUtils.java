package org.confluence.lib.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.wrapper.world.item.enchantment.EnchantmentHolder;
import org.mesdag.portlib.wrapper.world.item.enchantment.PortItemEnchantments;

public final class EnchantmentUtils {
    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantments, @Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        // 从物品堆栈中获取附魔信息，如果没有则使用空的附魔集合。
        PortItemEnchantments itemEnchantments = stack.getPortEnchantmentsOrDefault(PortItemEnchantments.EMPTY);

        for (Object2IntMap.Entry<EnchantmentHolder> entry : itemEnchantments.entrySet()) { // 获得物品的所有附魔
            if (entry.getKey().is(enchantments)) { // key是附魔，value是附魔的等级
                return entry.getIntValue();
            }
        }

        // 没有附魔则返回0。
        return 0;
    }
}
