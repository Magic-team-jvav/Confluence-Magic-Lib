package org.confluence.lib.common.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.confluence.lib.mixed.LibShapedRecipePattern;

import java.util.function.BiFunction;

public abstract class ShapedAmountRecipe4x<T extends MenuRecipeInput> extends AbstractAmountRecipe<T> {
    public final ShapedRecipePattern pattern;

    public ShapedAmountRecipe4x(ItemStack result, ShapedRecipePattern pattern) {
        super(result, pattern.ingredients());
        this.pattern = pattern;
        LibShapedRecipePattern.setNonSymmetricalMatching(pattern);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= pattern.width() && height >= pattern.height();
    }

    @Override
    public boolean matches(T input, Level pLevel) {
        return pattern.matches(input.asCraftingInput(false));
    }

    @Override
    public ItemStack assembleAndExtract(T input, HolderLookup.Provider registries) {
        consumeShaped(input, 4, 4, pattern);
        return assemble(input, registries);
    }

    @Override
    public boolean isIncomplete() {
        NonNullList<Ingredient> nonnulllist = getIngredients();
        return nonnulllist.isEmpty() || nonnulllist.stream().filter(ingredient -> !ingredient.isEmpty()).anyMatch(Ingredient::hasNoItems);
    }

    @Override
    protected int maxIngredientSize() {
        return 16;
    }

    public static <R extends ShapedAmountRecipe4x<?>> MapCodec<R> shapedSerializerMapCodec(BiFunction<ItemStack, ShapedRecipePattern, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern)
        ).apply(instance, factory));
    }

    public static <R extends ShapedAmountRecipe4x<?>> StreamCodec<RegistryFriendlyByteBuf, R> shapedSerializerSteamCodec(BiFunction<ItemStack, ShapedRecipePattern, R> factory) {
        return new StreamCodec<>() {
            @Override
            public R decode(RegistryFriendlyByteBuf buffer) {
                ItemStack itemstack = ItemStack.STREAM_CODEC.decode(buffer);
                ShapedRecipePattern shapedrecipepattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
                return factory.apply(itemstack, shapedrecipepattern);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buffer, R recipe) {
                ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
                ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            }
        };
    }
}
