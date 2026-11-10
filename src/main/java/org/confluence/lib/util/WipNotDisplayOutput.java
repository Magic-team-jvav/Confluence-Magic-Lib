package org.confluence.lib.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ItemLike;
import org.confluence.lib.common.LibTags;
import org.mesdag.portlib.registries.PortBlockRegistration;
import org.mesdag.portlib.registries.PortItemRegistration;
import org.mesdag.portlib.registries.PortRegistration;

public record WipNotDisplayOutput(
        CreativeModeTab.Output delegate
) implements CreativeModeTab.Output {
    private static boolean forceAllow = LibUtils.isDev();

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility) {
        if (displayable(stack)) {
            delegate.accept(stack, tabVisibility);
        }
    }

    public <I extends ItemLike> void acceptAll(PortRegistration<I> register, CreativeModeTab.TabVisibility tabVisibility) {
        register.getEntries().forEach(item -> accept(item.get(), tabVisibility));
    }

    public void acceptAll(PortItemRegistration register) {
        acceptAll(register, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public void acceptAll(PortBlockRegistration register) {
        acceptAll(register, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public static boolean displayable(ItemStack stack) {
        return forceAllow || (!stack.is(LibTags.Items.WIP) && !hasWipEnchantment(stack));
    }

    /// 附魔书共用 `minecraft:enchanted_book` 物品，没法用物品标签区分具体是哪一本，
    /// 所以额外看 `LibTags.Enchantments.WIP`：书上的附魔存在 `StoredEnchantments` 里，
    /// `EnchantmentHelper#getEnchantments` 会把两者一起读出来。
    private static boolean hasWipEnchantment(ItemStack stack) {
        for (Enchantment enchantment : EnchantmentHelper.getEnchantments(stack).keySet()) {
            Holder<Enchantment> holder = BuiltInRegistries.ENCHANTMENT.wrapAsHolder(enchantment);
            if (holder.is(LibTags.Enchantments.WIP)) return true;
        }
        return false;
    }

    public static void forceAllow() {
        forceAllow = true;
    }
}
