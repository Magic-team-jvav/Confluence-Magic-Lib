package org.confluence.lib.common.recipe;

import PortLib.extensions.java.util.List.PortListExtension;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectFunction;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import org.confluence.lib.util.LibStreamCodecUtils;
import org.jetbrains.annotations.ApiStatus;
import org.joml.Vector2i;
import org.mesdag.portlib.diff.Diff;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipe;
import org.mesdag.portlib.wrapper.world.item.crafting.PortRecipeInput;
import org.mesdag.portlib.wrapper.world.item.crafting.PortShapedRecipePattern;

import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;

public abstract class AbstractAmountRecipe<I extends PortRecipeInput> implements PortRecipe<I> {
    public static final MapCodec<NonNullList<Ingredient>> INGREDIENTS_CODEC = Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap(list -> {
        Ingredient[] ingredients = list.toArray(new Ingredient[0]);
        if (ingredients.length == 0) {
            return DataResult.error(() -> "No ingredients for recipe");
        } else {
            return DataResult.success(NonNullList.of(AmountIngredient.EMPTY, ingredients), Lifecycle.stable());
        }
    }, ingredients -> DataResult.success(ingredients, Lifecycle.stable()));
    private static final Object2ObjectFunction<Ingredient, Tuple<Integer, IntArraySet>> FUNCTION = I -> new Tuple<>(I instanceof AmountIngredient ai ? ai.amount() : 1, new IntArraySet());
    public final ItemStack result;
    public final NonNullList<Ingredient> ingredients;

    protected ResourceLocation id;

    protected AbstractAmountRecipe(ItemStack result, NonNullList<Ingredient> ingredients) {
        if (ingredients.size() > maxIngredientSize()) {
            throw new RuntimeException("Too many ingredients for '" + getGroup() + "' recipe. The maximum is: " + maxIngredientSize());
        }
        this.result = result;
        this.ingredients = ingredients;
    }

    @Diff
    @ApiStatus.NonExtendable
    @Override
    public void setId(ResourceLocation id) {
        this.id = id;
    }

    @Diff
    @ApiStatus.NonExtendable
    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return result;
    }

    @Override
    public boolean matches(I input, Level pLevel) {
        return matches(input.size(), input::getItem, ingredients);
    }

    public static boolean matches(int size, Int2ObjectFunction<ItemStack> getItemStackCallback, NonNullList<Ingredient> ingredients) {
        HashSet<Ingredient> matches = new HashSet<>();
        Object2IntOpenHashMap<Integer> requires2Count = new Object2IntOpenHashMap<>();
        outer:
        for (int j = 0; j < ingredients.size(); j++) {
            Ingredient ingredient = ingredients.get(j);
            for (int i = 0; i < size; i++) {
                ItemStack itemStack = getItemStackCallback.apply(i);
                if (itemStack.isEmpty()) continue;
                if (ingredient instanceof AmountIngredient ai) {
                    if (ai.ingredient().test(itemStack)) {
                        requires2Count.addTo(j, itemStack.getCount());
                        matches.add(ingredient);
                    }
                } else if (ingredient.test(itemStack)) {
                    matches.add(ingredient);
                    continue outer; // break;
                }
            }
        }
        if (matches.size() != ingredients.size()) return false;
        for (Object2IntMap.Entry<Integer> entry : requires2Count.object2IntEntrySet()) {
            Ingredient ingredient = ingredients.get(entry.getKey());
            if (!(ingredient instanceof AmountIngredient ai) || ai.amount() > entry.getIntValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(I container, RegistryAccess registryAccess) {
        return getResultItem(registryAccess).copy();
    }

    public ItemStack assembleAndExtract(I input, RegistryAccess registryAccess) {
        consumeShapeless(input, ingredients);
        return assemble(input, registryAccess);
    }

    public static void consumeShapeless(PortRecipeInput input, NonNullList<Ingredient> ingredients) {
        consumeShapeless(input.size(), input::getItem, ingredients);
    }

    public static void consumeShaped(PortRecipeInput input, int recipeWidth, int recipeHeight, PortShapedRecipePattern pattern) {
        if (pattern.data.isPresent()) {
            PortShapedRecipePattern.Data data = pattern.data.get();
            // 计算顶格
            int patternWidth = PortListExtension.getFirst(data.pattern()).length();
            int patternHeight = data.pattern().size();
            Vector2i patternTopLeft = findPatternTopLeft(data.pattern(), patternWidth, patternHeight);
            Vector2i containerTopLeft = findContainerTopLeft(input, recipeWidth, recipeHeight);
            // 抽取物品
            for (int i = 0; i < patternHeight; i++) {
                if (i >= patternHeight - patternTopLeft.y) continue;
                String row = data.pattern().get(i + patternTopLeft.y);
                int dy = (i + containerTopLeft.y) * recipeWidth;
                for (int j = 0; j < patternWidth; j++) {
                    if (j >= patternWidth - patternTopLeft.x) continue;
                    char c = row.charAt(j + patternTopLeft.x);
                    if (c == ' ') continue;
                    Ingredient ingredient = data.key().get(c);
                    ItemStack stack = input.getItem(j + containerTopLeft.x + dy);
                    stack.shrink(AmountIngredient.getAmount(ingredient));
                }
            }
        }
    }

    public static void consumeShapeless(int pContainerSize, Int2ObjectFunction<ItemStack> getItemStackCallback, NonNullList<Ingredient> ingredients) {
        Object2ObjectOpenHashMap<Ingredient, Tuple<Integer, IntArraySet>> requires2Slots = new Object2ObjectOpenHashMap<>();
        outer:
        for (Ingredient ingredient : ingredients) {
            for (int i = 0; i < pContainerSize; i++) {
                ItemStack itemStack = getItemStackCallback.apply(i);
                if (itemStack.isEmpty()) continue;
                if (ingredient instanceof AmountIngredient a) {
                    if (a.amount() == requires2Slots.computeIfAbsent(ingredient, FUNCTION).getB().size()) {
                        continue outer;
                    }
                    if (a.ingredient().test(itemStack)) {
                        requires2Slots.computeIfAbsent(ingredient, FUNCTION).getB().add(i);
                    }
                } else if (ingredient.test(itemStack)) {
                    requires2Slots.computeIfAbsent(ingredient, FUNCTION).getB().add(i);
                }
            }
        }
        for (Tuple<Integer, IntArraySet> tuple : requires2Slots.values()) {
            int requires = tuple.getA();
            int[] slots = tuple.getB().toIntArray();
            int avg = requires / slots.length;
            int rem = requires % slots.length;
            boolean shouldConsumeRem = false;
            if (rem > 0) {
                shouldConsumeRem = true;
                rem += avg;
            }
            for (int slot : slots) {
                ItemStack itemStack = getItemStackCallback.apply(slot);
                if (shouldConsumeRem && itemStack.getCount() >= rem) {
                    itemStack.shrink(rem);
                    shouldConsumeRem = false;
                } else {
                    itemStack.shrink(avg);
                }
            }
        }
    }

    @Override
    public boolean canCraftInDimensions(int i, int i1) {
        return true;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return ingredients;
    }

    protected abstract int maxIngredientSize();

    @Override
    public abstract String getGroup();

    @Override
    public abstract ItemStack getToastSymbol();

    public static Vector2i findPatternTopLeft(List<String> pattern, int patternWidth, int patternHeight) {
        int x = patternWidth - 1, y = patternHeight - 1;
        for (int i = 0; i < patternHeight; i++) {
            String s = pattern.get(i);
            for (int j = 0; j < patternWidth; j++) {
                if (s.charAt(j) != ' ') {
                    if (x > j) x = j;
                    if (y > i) y = i;
                }
            }
        }
        return new Vector2i(x, y);
    }

    public static Vector2i findContainerTopLeft(PortRecipeInput container, int recipeWidth, int recipeHeight) {
        int x = recipeWidth - 1, y = recipeHeight - 1;
        for (int i = 0; i < recipeHeight; i++) {
            int dy = i * recipeWidth;
            for (int j = 0; j < recipeWidth; j++) {
                if (!container.getItem(j + dy).isEmpty()) {
                    if (x > j) x = j;
                    if (y > i) y = i;
                }
            }
        }
        return new Vector2i(x, y);
    }

    public static <R extends AbstractAmountRecipe<?>> MapCodec<R> shapelessSerializerMapCodec(BiFunction<ItemStack, NonNullList<Ingredient>, R> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                INGREDIENTS_CODEC.forGetter(AbstractAmountRecipe::getIngredients)
        ).apply(instance, factory));
    }

    public static <R extends AbstractAmountRecipe<?>> PortStreamCodec<PortRegistryFriendlyByteBuf, R> shapelessSerializerSteamCodec(BiFunction<ItemStack, NonNullList<Ingredient>, R> factory) {
        return PortStreamCodec.composite(
                ItemStack.STREAM_CODEC, r -> r.result,
                LibStreamCodecUtils.INGREDIENTS, AbstractAmountRecipe::getIngredients,
                factory
        );
    }
}
