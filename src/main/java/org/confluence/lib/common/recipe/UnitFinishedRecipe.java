package org.confluence.lib.common.recipe;

import com.google.gson.JsonObject;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.diff.Diff;

@Diff
public class UnitFinishedRecipe implements FinishedRecipe {
    private final Recipe<?> recipe;

    public UnitFinishedRecipe(Recipe<?> recipe) {
        this.recipe = recipe;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {}

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public RecipeSerializer<?> getType() {
        return recipe.getSerializer();
    }

    @Override
    public @Nullable JsonObject serializeAdvancement() {
        return null;
    }

    @Override
    public @Nullable ResourceLocation getAdvancementId() {
        return null;
    }
}
