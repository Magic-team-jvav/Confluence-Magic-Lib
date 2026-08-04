package org.confluence.lib.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.FuelValues;

public class ForgeFuelSlot extends Slot {
    private final RecipeType<?> recipeType;
    private final FuelValues fuelValues;

    public ForgeFuelSlot(RecipeType<?> recipeType, FuelValues fuelValues, Container furnaceContainer, int slot, int xPosition, int yPosition) {
        super(furnaceContainer, slot, xPosition, yPosition);
        this.recipeType = recipeType;
        this.fuelValues = fuelValues;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return stack.getBurnTime(recipeType, fuelValues) > 0 || isBucket(stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return isBucket(stack) ? 1 : super.getMaxStackSize(stack);
    }

    public static boolean isBucket(ItemStack stack) {
        return stack.is(Items.BUCKET);
    }
}
