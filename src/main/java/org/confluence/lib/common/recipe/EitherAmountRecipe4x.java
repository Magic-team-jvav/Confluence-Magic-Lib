package org.confluence.lib.common.recipe;

import PortLib.extensions.net.minecraft.world.item.ItemStack.PortItemStackExtension;
import PortLib.extensions.net.minecraft.world.item.crafting.Ingredient.PortIngredientExtension;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.confluence.lib.util.LibStreamCodecUtils;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.world.item.crafting.PortShapedRecipePattern;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class EitherAmountRecipe4x<I extends MenuRecipeInput> extends AbstractAmountRecipe<I> {
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, Either<PortShapedRecipePattern, NonNullList<Ingredient>>> EITHER_CODEC = PortByteBufCodecs.either(PortShapedRecipePattern.STREAM_CODEC, LibStreamCodecUtils.INGREDIENTS);
    public final Either<PortShapedRecipePattern, NonNullList<Ingredient>> either;

    public EitherAmountRecipe4x(ItemStack result, PortShapedRecipePattern pattern) {
        super(result, pattern.ingredients());
        this.either = Either.left(pattern);
        pattern.setNonSymmetricalMatching();
    }

    public EitherAmountRecipe4x(ItemStack result, NonNullList<Ingredient> ingredients) {
        super(result, ingredients);
        this.either = Either.right(ingredients);
    }

    public EitherAmountRecipe4x(ItemStack result, Either<PortShapedRecipePattern, NonNullList<Ingredient>> either) {
        super(result, either.map(PortShapedRecipePattern::ingredients, Function.identity()));
        this.either = either;
        either.ifLeft(PortShapedRecipePattern::setNonSymmetricalMatching);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return either.map(shaped -> width >= shaped.width() && height >= shaped.height(), shapeless -> true);
    }

    @Override
    public boolean matches(I input, Level level) {
        return either.map(
                shaped -> shaped.matches(input.asCraftingInput(false)),
                shapeless -> matches(input.size(), input::getItem, shapeless)
        );
    }

    @Override
    public ItemStack assembleAndExtract(I input, RegistryAccess registryAccess) {
        either
                .ifLeft(shaped -> consumeShaped(input, 4, 4, shaped))
                .ifRight(shapeless -> consumeShapeless(input, shapeless));
        return assemble(input, registryAccess);
    }

    @Override
    public boolean isIncomplete() {
        return either.map(
                shaped -> shaped.ingredients().isEmpty() || shaped.ingredients().stream().filter(ingredient -> !ingredient.isEmpty()).anyMatch(PortIngredientExtension::hasNoItems),
                shapeless -> shapeless.isEmpty() || shapeless.stream().anyMatch(PortIngredientExtension::hasNoItems)
        );
    }

    @Override
    protected int maxIngredientSize() {
        return 16;
    }

    public static <R extends EitherAmountRecipe4x<?>> MapCodec<R> shapedSerializerMapCodec(BiFunction<ItemStack, PortShapedRecipePattern, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                PortItemStackExtension.strictCodec().fieldOf("result").forGetter(recipe -> recipe.result),
                PortShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.either.left().orElseThrow())
        ).apply(instance, factory));
    }

    public static <R extends EitherAmountRecipe4x<?>> PortStreamCodec<PortRegistryFriendlyByteBuf, R> shapedSerializerSteamCodec(BiFunction<ItemStack, PortShapedRecipePattern, R> factory) {
        return PortStreamCodec.composite(
                PortItemStackExtension.streamCodec(), r -> r.result,
                PortShapedRecipePattern.STREAM_CODEC, r -> r.either.left().orElseThrow(),
                factory
        );
    }

    public static <R extends EitherAmountRecipe4x<?>> MapCodec<R> eitherSerializerMapCodec(BiFunction<ItemStack, Either<PortShapedRecipePattern, NonNullList<Ingredient>>, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                PortItemStackExtension.strictCodec().fieldOf("result").forGetter(recipe -> recipe.result),
                Codec.mapEither(PortShapedRecipePattern.MAP_CODEC, INGREDIENTS_CODEC).forGetter(recipe -> recipe.either)
        ).apply(instance, factory));
    }

    public static <R extends EitherAmountRecipe4x<?>> PortStreamCodec<PortRegistryFriendlyByteBuf, R> eitherSerializerStreamCodec(BiFunction<ItemStack, Either<PortShapedRecipePattern, NonNullList<Ingredient>>, R> factory) {
        return PortStreamCodec.composite(
                PortItemStackExtension.streamCodec(), r -> r.result,
                EITHER_CODEC, r -> r.either,
                factory
        );
    }
}
