package org.confluence.lib.common.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.common.recipe.AbstractAmountRecipe;
import org.confluence.lib.common.recipe.EitherAmountRecipe4x;
import org.confluence.lib.common.recipe.MenuRecipeInput;

public class ToggleAmountResultSlot<R extends AbstractAmountRecipe<?>> extends AmountResultSlot<R> implements IToggleSlot {
    public boolean isActive = true;

    public ToggleAmountResultSlot(MenuRecipeInput input, Container container, int slot, int x, int y) {
        super(input, container, slot, x, y);
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void setEnable(boolean enable) {
        this.isActive = enable;
    }

    @Override
    public boolean isEnabled() {
        return isActive;
    }

    public static class Setup<R extends AbstractAmountRecipe<?>> extends ToggleAmountResultSlot<R> {
        private final Runnable setup;

        public Setup(MenuRecipeInput input, Container container, int slot, int x, int y, Runnable setup) {
            super(input, container, slot, x, y);
            this.setup = setup;
        }

        @Override
        protected void updateMenu() {
            setup.run();
        }
    }

    public static class For4x<R extends EitherAmountRecipe4x<?>> extends Setup<R> {
        public For4x(MenuRecipeInput input, Container container, int slot, int x, int y, Runnable setup) {
            super(input, container, slot, x, y, setup);
        }

        @Override
        public void onTake(Player player, ItemStack stack) {
            if (recipe != null) {
                recipe.either
                        .ifLeft(pattern -> AbstractAmountRecipe.consumeShaped(input, 4, 4, pattern))
                        .ifRight(ingredients -> AbstractAmountRecipe.consumeShapeless(input, ingredients));
                input.setChanged();
                updateMenu();
            }
        }
    }
}
