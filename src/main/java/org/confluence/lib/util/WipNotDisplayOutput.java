package org.confluence.lib.util;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
        return forceAllow || !stack.is(LibTags.Items.WIP);
    }

    public static void forceAllow() {
        forceAllow = true;
    }
}
