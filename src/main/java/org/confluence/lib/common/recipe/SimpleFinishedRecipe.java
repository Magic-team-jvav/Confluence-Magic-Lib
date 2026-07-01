package org.confluence.lib.common.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.diff.Diff;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipe;

@Diff
public class SimpleFinishedRecipe<R extends PortRecipe<?>> implements FinishedRecipe {
    private final R recipe;

    public SimpleFinishedRecipe(R recipe) {
        this.recipe = recipe;
    }

    public SimpleFinishedRecipe(ResourceLocation id, R recipe) {
        this(recipe);
        recipe.setId(id);
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
        MapCodec<R> codec = (MapCodec<R>) getType().getCodec();
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        codec.encode(recipe, ops, codec.compressedBuilder(ops)).build(json);
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public SimpleRecipeSerializer<?> getType() {
        return (SimpleRecipeSerializer<?>) recipe.getType();
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
