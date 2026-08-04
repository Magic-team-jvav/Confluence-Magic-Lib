package org.confluence.lib.common.recipe;

import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.confluence.lib.util.LibStreamCodecUtils;

import java.util.List;
import java.util.Optional;

public abstract class EnvironmentAmountRecipe extends AbstractAmountRecipe<EnvironmentRecipeInput> {
    protected final EnvironmentLevelAccess.Matcher environment;

    protected EnvironmentAmountRecipe(ItemStackTemplate result, List<Optional<Ingredient>> ingredients, EnvironmentLevelAccess.Matcher environment) {
        super(result, ingredients);
        this.environment = environment;
    }

    public EnvironmentLevelAccess.Matcher getEnvironment() {
        return environment;
    }

    @Override
    public boolean matches(EnvironmentRecipeInput input, Level level) {
        return environment.matches(input.getAccess()) && super.matches(input, level);
    }

    public static <R extends EnvironmentAmountRecipe> MapCodec<R> environmentShapelessSerializerMapCodec(Function3<ItemStackTemplate, List<Optional<Ingredient>>, EnvironmentLevelAccess.Matcher, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(EnvironmentAmountRecipe::getTemplate),
                INGREDIENTS_CODEC.forGetter(EnvironmentAmountRecipe::getIngredients),
                EnvironmentLevelAccess.Matcher.MAP_CODEC.forGetter(EnvironmentAmountRecipe::getEnvironment)
        ).apply(instance, factory));
    }

    public static <R extends EnvironmentAmountRecipe> StreamCodec<RegistryFriendlyByteBuf, R> environmentShapelessSerializerSteamCodec(Function3<ItemStackTemplate, List<Optional<Ingredient>>, EnvironmentLevelAccess.Matcher, R> factory) {
        return StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, EnvironmentAmountRecipe::getTemplate,
                LibStreamCodecUtils.INGREDIENTS, EnvironmentAmountRecipe::getIngredients,
                EnvironmentLevelAccess.Matcher.STREAM_CODEC, EnvironmentAmountRecipe::getEnvironment,
                factory
        );
    }
}
