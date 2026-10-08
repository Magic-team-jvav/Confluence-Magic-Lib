package org.confluence.lib.common.recipe;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.confluence.lib.util.LibStreamCodecUtils;

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
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(R::getResult),
                INGREDIENTS_CODEC.forGetter(R::getIngredients),
                EnvironmentLevelAccess.Matcher.MAP_CODEC.forGetter(R::getEnvironment)
        ).apply(instance, factory));
    }

    public static <R extends EnvironmentAmountRecipe> StreamCodec<RegistryFriendlyByteBuf, R> environmentShapelessSerializerSteamCodec(Function3<ItemStack, NonNullList<Ingredient>, EnvironmentLevelAccess.Matcher, R> factory) {
        return StreamCodec.composite(
                ItemStack.STREAM_CODEC, R::getResult,
                LibStreamCodecUtils.INGREDIENTS, R::getIngredients,
                EnvironmentLevelAccess.Matcher.STREAM_CODEC, R::getEnvironment,
                factory
        );
    }
}
