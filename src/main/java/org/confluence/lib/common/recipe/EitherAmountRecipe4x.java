package org.confluence.lib.common.recipe;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.confluence.lib.mixed.ILibShapedRecipePattern;
import org.confluence.lib.util.LibStreamCodecUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class EitherAmountRecipe4x<I extends MenuRecipeInput> extends AbstractAmountRecipe<I> {
    public static final StreamCodec<RegistryFriendlyByteBuf, Either<ShapedRecipePattern, List<Optional<Ingredient>>>> EITHER_CODEC = ByteBufCodecs.either(ShapedRecipePattern.STREAM_CODEC, LibStreamCodecUtils.INGREDIENTS);
    public final Either<ShapedRecipePattern, List<Optional<Ingredient>>> either;

    public EitherAmountRecipe4x(ItemStackTemplate result, ShapedRecipePattern pattern) {
        super(result, pattern.ingredients());
        this.either = Either.left(pattern);
        ILibShapedRecipePattern.setNonSymmetricalMatching(pattern);
    }

    public EitherAmountRecipe4x(ItemStackTemplate result, List<Optional<Ingredient>> ingredients) {
        super(result, ingredients);
        this.either = Either.right(ingredients);
    }

    public EitherAmountRecipe4x(ItemStackTemplate result, Either<ShapedRecipePattern, List<Optional<Ingredient>>> either) {
        super(result, either.map(ShapedRecipePattern::ingredients, Function.identity()));
        this.either = either;
        either.ifLeft(ILibShapedRecipePattern::setNonSymmetricalMatching);
    }

    @Override
    public boolean matches(I input, Level level) {
        return either.map(
                shaped -> shaped.matches(input.asCraftingInput(false)),
                shapeless -> matches(input.size(), input::getItem, shapeless)
        );
    }

    @Override
    public ItemStack assembleAndExtract(I input, HolderLookup.Provider registries) {
        either
                .ifLeft(shaped -> consumeShaped(input, 4, 4, shaped))
                .ifRight(shapeless -> consumeShapeless(input, shapeless));
        return assemble(input);
    }

    @Override
    protected int maxIngredientSize() {
        return 16;
    }

    public static <R extends EitherAmountRecipe4x<?>> MapCodec<R> shapedSerializerMapCodec(BiFunction<ItemStackTemplate, ShapedRecipePattern, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(EitherAmountRecipe4x::getTemplate),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.either.left().orElseThrow())
        ).apply(instance, factory));
    }

    public static <R extends EitherAmountRecipe4x<?>> StreamCodec<RegistryFriendlyByteBuf, R> shapedSerializerSteamCodec(BiFunction<ItemStackTemplate, ShapedRecipePattern, R> factory) {
        return StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, EitherAmountRecipe4x::getTemplate,
                ShapedRecipePattern.STREAM_CODEC, r -> r.either.left().orElseThrow(),
                factory
        );
    }

    public static <R extends EitherAmountRecipe4x<?>> MapCodec<R> eitherSerializerMapCodec(BiFunction<ItemStackTemplate, Either<ShapedRecipePattern, List<Optional<Ingredient>>>, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(EitherAmountRecipe4x::getTemplate),
                Codec.mapEither(ShapedRecipePattern.MAP_CODEC, INGREDIENTS_CODEC).forGetter(recipe -> recipe.either)
        ).apply(instance, factory));
    }

    public static <R extends EitherAmountRecipe4x<?>> StreamCodec<RegistryFriendlyByteBuf, R> eitherSerializerStreamCodec(BiFunction<ItemStackTemplate, Either<ShapedRecipePattern, List<Optional<Ingredient>>>, R> factory) {
        return StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, EitherAmountRecipe4x::getTemplate,
                EITHER_CODEC, r -> r.either,
                factory
        );
    }
}
