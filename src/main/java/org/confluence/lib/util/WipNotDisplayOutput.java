package org.confluence.lib.util;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
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
        return forceAllow || !stack.is(LibTags.Items.WIP);
    }

    public static void forceAllow() {
        forceAllow = true;
    }
}
