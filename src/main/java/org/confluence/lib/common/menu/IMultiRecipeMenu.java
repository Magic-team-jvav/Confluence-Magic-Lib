package org.confluence.lib.common.menu;

import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.List;

public interface IMultiRecipeMenu<R extends Recipe<?>> {
    void setRecipes(List<RecipeHolder<R>> recipes);

    List<RecipeHolder<R>> getRecipes();

    RecipeType<R> getRecipeType();
}
