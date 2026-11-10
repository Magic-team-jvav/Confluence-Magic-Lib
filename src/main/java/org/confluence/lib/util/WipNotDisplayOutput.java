package org.confluence.lib.util;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.confluence.lib.common.LibTags;

public record WipNotDisplayOutput(CreativeModeTab.Output delegate) implements CreativeModeTab.Output {
    private static boolean forceAllow = LibUtils.isDev();

    @Override
    public void accept(ItemStack stack, CreativeModeTab.TabVisibility tabVisibility) {
        if (displayable(stack)) {
            delegate.accept(stack, tabVisibility);
        }
    }

    public void acceptAll(DeferredRegister.Items register, CreativeModeTab.TabVisibility tabVisibility) {
        register.getEntries().forEach(item -> accept(item.get(), tabVisibility));
    }

    public void acceptAll(DeferredRegister.Items register) {
        acceptAll(register, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public void acceptAll(DeferredRegister.Blocks register, CreativeModeTab.TabVisibility tabVisibility) {
        register.getEntries().forEach(item -> accept(item.get(), tabVisibility));
    }

    public void acceptAll(DeferredRegister.Blocks register) {
        acceptAll(register, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
    }

    public static boolean displayable(ItemStack stack) {
        return forceAllow || (!stack.is(LibTags.Items.WIP) && !hasWipEnchantment(stack));
    }

    /// 附魔书共用 `minecraft:enchanted_book` 物品，没法用物品标签区分具体是哪一本，
    /// 所以额外看 `LibTags.Enchantments.WIP`：书上的附魔（`STORED_ENCHANTMENTS`）或物品上的附魔
    /// （`ENCHANTMENTS`）命中该标签就不显示。
    private static boolean hasWipEnchantment(ItemStack stack) {
        return hasWipEnchantment(stack.get(DataComponents.ENCHANTMENTS))
                || hasWipEnchantment(stack.get(DataComponents.STORED_ENCHANTMENTS));
    }

    private static boolean hasWipEnchantment(ItemEnchantments enchantments) {
        if (enchantments == null || enchantments.isEmpty()) return false;
        for (var entry : enchantments.entrySet()) {
            Holder<Enchantment> enchantment = entry.getKey();
            if (enchantment.is(LibTags.Enchantments.WIP)) return true;
        }
        return false;
    }

    public static void forceAllow() {
        forceAllow = true;
    }
}
