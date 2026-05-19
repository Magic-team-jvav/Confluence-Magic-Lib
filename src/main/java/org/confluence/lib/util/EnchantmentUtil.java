package org.confluence.lib.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public final class EnchantmentUtil {
    public static int getEnchantmentLevel(ResourceKey<Enchantment> enchantments, @Nullable ItemStack stack) {
        return EnchantmentUtils.getEnchantmentLevel(enchantments, stack);
    }
}
