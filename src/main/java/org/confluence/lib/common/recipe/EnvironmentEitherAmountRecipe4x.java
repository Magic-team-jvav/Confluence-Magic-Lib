package org.confluence.lib.common.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Function3;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

public abstract class EnvironmentEitherAmountRecipe4x extends EitherAmountRecipe4x<EnvironmentRecipeInput> {
    protected final EnvironmentLevelAccess.Matcher environment;

    public EnvironmentEitherAmountRecipe4x(ItemStackTemplate result, ShapedRecipePattern pattern, EnvironmentLevelAccess.Matcher environment) {
        super(result, pattern);
        this.environment = environment;
    }

    public EnvironmentEitherAmountRecipe4x(ItemStackTemplate result, List<Optional<Ingredient>> ingredients, EnvironmentLevelAccess.Matcher environment) {
        super(result, ingredients);
        this.environment = environment;
    }

    public EnvironmentEitherAmountRecipe4x(ItemStackTemplate result, Either<ShapedRecipePattern, List<Optional<Ingredient>>> either, EnvironmentLevelAccess.Matcher environment) {
        super(result, either);
        this.environment = environment;
    }

    public EnvironmentLevelAccess.Matcher getEnvironment() {
        return environment;
    }

    @Override
    public boolean matches(EnvironmentRecipeInput input, Level level) {
        return environment.matches(input.getAccess()) && super.matches(input, level);
    }

    public static <R extends EnvironmentEitherAmountRecipe4x> MapCodec<R> environmentShapedSerializerMapCodec(Function3<ItemStackTemplate, ShapedRecipePattern, EnvironmentLevelAccess.Matcher, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(EnvironmentEitherAmountRecipe4x::getTemplate),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.either.left().orElseThrow()),
                EnvironmentLevelAccess.Matcher.MAP_CODEC.forGetter(EnvironmentEitherAmountRecipe4x::getEnvironment)
        ).apply(instance, factory));
    }

    public static <R extends EnvironmentEitherAmountRecipe4x> StreamCodec<RegistryFriendlyByteBuf, R> environmentShapedSerializerSteamCodec(Function3<ItemStackTemplate, ShapedRecipePattern, EnvironmentLevelAccess.Matcher, R> factory) {
        return StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, r -> r.template,
                ShapedRecipePattern.STREAM_CODEC, r -> r.either.left().orElseThrow(),
                EnvironmentLevelAccess.Matcher.STREAM_CODEC, EnvironmentEitherAmountRecipe4x::getEnvironment,
                factory
        );
    }

    public static <R extends EnvironmentEitherAmountRecipe4x> MapCodec<R> environmentEitherSerializerMapCodec(Function3<ItemStackTemplate, Either<ShapedRecipePattern, List<Optional<Ingredient>>>, EnvironmentLevelAccess.Matcher, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(EnvironmentEitherAmountRecipe4x::getTemplate),
                Codec.mapEither(ShapedRecipePattern.MAP_CODEC, INGREDIENTS_CODEC).forGetter(recipe -> recipe.either),
                EnvironmentLevelAccess.Matcher.MAP_CODEC.forGetter(EnvironmentEitherAmountRecipe4x::getEnvironment)
        ).apply(instance, factory));
    }

    public static <R extends EnvironmentEitherAmountRecipe4x> StreamCodec<RegistryFriendlyByteBuf, R> environmentEitherSerializerStreamCodec(Function3<ItemStackTemplate, Either<ShapedRecipePattern, List<Optional<Ingredient>>>, EnvironmentLevelAccess.Matcher, R> factory) {
        return StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, EnvironmentEitherAmountRecipe4x::getTemplate,
                EITHER_CODEC, r -> r.either,
                EnvironmentLevelAccess.Matcher.STREAM_CODEC, EnvironmentEitherAmountRecipe4x::getEnvironment,
                factory
        );
    }
}
