package org.confluence.lib.common.recipe;

import net.minecraft.world.item.ItemStack;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipeInput;

public class ArrayRecipeInput implements PortRecipeInput {
    private final ItemStack[] itemStacks;

    public ArrayRecipeInput(ItemStack[] itemStacks) {
        this.itemStacks = itemStacks;
    }

    @Override
    public ItemStack getItem(int index) {
        return itemStacks[index];
    }

    @Override
    public int size() {
        return itemStacks.length;
    }
}
