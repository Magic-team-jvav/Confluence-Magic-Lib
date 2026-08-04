package org.confluence.lib.common.recipe;

import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;
import java.util.Optional;

public interface IRecipeWithIngredients<T extends RecipeInput> extends Recipe<T> {
    List<Optional<Ingredient>> getIngredients();
}
