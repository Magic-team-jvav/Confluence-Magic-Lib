package org.confluence.lib.common.recipe;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.confluence.lib.util.LibStreamCodecUtils;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortStreamCodec;

public abstract class EnvironmentAmountRecipe extends AbstractAmountRecipe<EnvironmentRecipeInput> {
    protected final EnvironmentLevelAccess.Matcher environment;

    protected EnvironmentAmountRecipe(ItemStack result, NonNullList<Ingredient> ingredients, EnvironmentLevelAccess.Matcher environment) {
        super(result, ingredients);
        this.environment = environment;
    }

    public EnvironmentLevelAccess.Matcher getEnvironment() {
        return environment;
    }

    @Override
    public boolean matches(EnvironmentRecipeInput input, Level pLevel) {
        return environment.matches(input.getAccess()) && super.matches(input, pLevel);
    }

    public static <R extends EnvironmentAmountRecipe> MapCodec<R> environmentShapelessSerializerMapCodec(Function3<ItemStack, NonNullList<Ingredient>, EnvironmentLevelAccess.Matcher, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(AbstractAmountRecipe::getResult),
                INGREDIENTS_CODEC.forGetter(AbstractAmountRecipe::getIngredients),
                EnvironmentLevelAccess.Matcher.MAP_CODEC.forGetter(EnvironmentAmountRecipe::getEnvironment)
        ).apply(instance, factory));
    }

    public static <R extends EnvironmentAmountRecipe> PortStreamCodec<PortRegistryFriendlyByteBuf, R> environmentShapelessSerializerSteamCodec(Function3<ItemStack, NonNullList<Ingredient>, EnvironmentLevelAccess.Matcher, R> factory) {
        return PortStreamCodec.composite(
                ItemStack.STREAM_CODEC, AbstractAmountRecipe::getResult,
                LibStreamCodecUtils.INGREDIENTS, AbstractAmountRecipe::getIngredients,
                EnvironmentLevelAccess.Matcher.STREAM_CODEC, EnvironmentAmountRecipe::getEnvironment,
                factory
        );
    }
}
