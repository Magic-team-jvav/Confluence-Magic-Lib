package org.confluence.lib.common.recipe;

import PortLib.extensions.com.mojang.serialization.DataResult.PortDataResultExtension;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.diff.Diff;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipe;

import java.util.Map;

@SuppressWarnings("unchecked")
@Diff
public final class SimpleFinishedRecipe<R extends PortRecipe<?>> implements FinishedRecipe {
    private final R recipe;
    private final Codec<R> codec;
    private @Nullable HolderLookup.Provider provider;

    public SimpleFinishedRecipe(R recipe) {
        this.recipe = recipe;
        this.codec = (Codec<R>) getType().getCodec().codec();
    }

    public SimpleFinishedRecipe(ResourceLocation id, R recipe) {
        this(recipe);
        recipe.setId(id);
    }

    public SimpleFinishedRecipe(ResourceLocation id, R recipe, @Nullable HolderLookup.Provider provider) {
        this(id, recipe);
        this.provider = provider;
    }

    @Override
    public void serializeRecipeData(JsonObject json) {
        DynamicOps<JsonElement> ops;
        if (provider == null) {
            ops = JsonOps.INSTANCE;
        } else {
            ops = provider.createSerializationContext(JsonOps.INSTANCE);
        }
        PortDataResultExtension.ifSuccess(codec.encodeStart(ops, recipe), r -> {
            for (Map.Entry<String, JsonElement> entry : r.getAsJsonObject().entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
        });
    }

    @Override
    public ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public SimpleRecipeSerializer<?> getType() {
        return (SimpleRecipeSerializer<?>) recipe.getSerializer();
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
