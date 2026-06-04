package org.confluence.lib.common.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import org.mesdag.portlib.network.codec.PortStreamCodec;

public abstract class SimpleRecipeSerializer<T extends Recipe<?>> implements RecipeSerializer<T> {
    private MapCodec<T> codec;
    private PortStreamCodec<RegistryFriendlyByteBuf, T> streamCodec;

    @Override
    public final MapCodec<T> codec() {
        if (codec == null) this.codec = getCodec();
        return codec;
    }

    protected abstract MapCodec<T> getCodec();

    @Override
    public final PortStreamCodec<RegistryFriendlyByteBuf, T> streamCodec() {
        if (streamCodec == null) this.streamCodec = getStreamCodec();
        return streamCodec;
    }

    protected abstract PortStreamCodec<RegistryFriendlyByteBuf, T> getStreamCodec();
}
