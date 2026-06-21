package org.confluence.lib.common.recipe;

import PortLib.extensions.com.mojang.serialization.DataResult.PortDataResultExtension;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipe;

public abstract class SimpleRecipeSerializer<R extends PortRecipe<?>> implements RecipeSerializer<R> {
    private MapCodec<R> codec;
    private PortStreamCodec<PortRegistryFriendlyByteBuf, R> streamCodec;

    @ApiStatus.NonExtendable
    @Override
    public R fromJson(ResourceLocation recipeId, JsonObject serializedRecipe) {
        if (codec == null) {
            codec = getCodec();
        }
        MapLike<JsonElement> mapLike = PortDataResultExtension.getOrThrow(JsonOps.INSTANCE.getMap(serializedRecipe));
        R r = PortDataResultExtension.getOrThrow(codec.decode(JsonOps.INSTANCE, mapLike));
        r.setId(recipeId);
        return r;
    }

    @ApiStatus.NonExtendable
    @Override
    public @Nullable R fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
        if (streamCodec == null) {
            streamCodec = getStreamCodec();
        }
        PortRegistryFriendlyByteBuf buf = buffer.wrap();
        R r = streamCodec.decode(buf);
        r.setId(recipeId);
        return r;
    }

    @ApiStatus.NonExtendable
    @Override
    public void toNetwork(FriendlyByteBuf buffer, R recipe) {
        if (streamCodec == null) {
            streamCodec = getStreamCodec();
        }
        PortRegistryFriendlyByteBuf buf = buffer.wrap();
        streamCodec.encode(buf, recipe);
    }

    protected abstract MapCodec<R> getCodec();

    protected abstract PortStreamCodec<PortRegistryFriendlyByteBuf, R> getStreamCodec();
}
