package org.confluence.lib.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;

public class RenderSelectedItemNameEvent extends Event {
    private final ItemStack stack;
    private final Component original;
    private Component override;

    public RenderSelectedItemNameEvent(ItemStack stack, Component original) {
        this.stack = stack;
        this.original = original;
        this.override = original;
    }

    public ItemStack getStack() {
        return stack;
    }

    public Component getOriginal() {
        return original;
    }

    public void setOverride(Component override) {
        this.override = override;
    }

    public Component getOverride() {
        return override;
    }
}
