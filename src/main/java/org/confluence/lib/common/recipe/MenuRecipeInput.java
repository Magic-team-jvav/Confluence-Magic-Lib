package org.confluence.lib.common.recipe;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.mesdag.portlib.wrapper.world.item.crafting.PortCraftingInput;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipeInput;

public class MenuRecipeInput extends SimpleContainer implements PortRecipeInput {
    private final AbstractContainerMenu menu;
    private PortCraftingInput craftingInput;

    public MenuRecipeInput(AbstractContainerMenu menu, int size) {
        super(size);
        this.menu = menu;
    }

    @Override
    public int size() {
        return getContainerSize();
    }

    @Override
    public void setChanged() {
        super.setChanged();
        menu.slotsChanged(this);
    }

    public PortCraftingInput asCraftingInput(boolean update) {
        if (update || craftingInput == null) {
            this.craftingInput = PortCraftingInput.of(4, 4, getItems());
        }
        return craftingInput;
    }
}
