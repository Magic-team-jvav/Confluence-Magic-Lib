package org.confluence.lib.common.recipe;

import PortLib.extensions.com.mojang.serialization.Codec.PortCodecExtension;
import PortLib.extensions.net.minecraft.world.item.crafting.Ingredient.PortIngredientExtension;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.common.crafting.PortCustomIngredient;
import org.mesdag.portlib.wrapper.common.crafting.PortIngredientType;

import java.util.Arrays;
import java.util.stream.Stream;

public final class AmountIngredient extends PortCustomIngredient {
    public static final Ingredient EMPTY = new AmountIngredient(Ingredient.EMPTY, 0);
    public static final MapCodec<AmountIngredient> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            PortCodecExtension.lenientOptionalFieldOf(PortIngredientExtension.codec(), "ingredient", Ingredient.EMPTY).forGetter(AmountIngredient::ingredient),
            PortCodecExtension.lenientOptionalFieldOf(ExtraCodecs.POSITIVE_INT, "count", 0).forGetter(AmountIngredient::amount)
    ).apply(instance, AmountIngredient::new));
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, AmountIngredient> STREAM_CODEC = PortByteBufCodecs.fromCodecWithRegistries(CODEC.codec());

    private final Ingredient ingredient;
    private final int amount;

    public AmountIngredient(Ingredient ingredient, int amount) {
        this.ingredient = ingredient;
        this.amount = amount;
    }

    public Ingredient ingredient() { return ingredient; }
    public int amount() { return amount; }

    @Override
    public Stream<ItemStack> getItemStream() {
        return Arrays.stream(ingredient.getItems()).peek(itemStack -> itemStack.setCount(amount));
    }

    @Override
    public boolean test(@Nullable ItemStack stack) {
        if (stack == null) return false;
        if (stack.getCount() < amount) return false;
        return ingredient.test(stack);
    }

    @Override
    public boolean isSimple() {
        return false;
    }

    @Override
    public PortIngredientType<AmountIngredient> getIngredientType() {
        return ConfluenceMagicLib.AMOUNT_INGREDIENT_TYPE.get();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (o instanceof AmountIngredient a) return ingredient.equals(a.ingredient) && amount == a.amount;
        return false;
    }

    @Override
    public int hashCode() {
        return 31 * ingredient.hashCode() + amount;
    }

    public static int getAmount(Ingredient ingredient) {
        return ingredient instanceof AmountIngredient ai ? ai.amount : 1;
    }

    public static Ingredient of(int amount, Ingredient ingredient) {
        return new AmountIngredient(ingredient, amount).toVanilla();
    }

    public static Ingredient of(int amount, ItemLike... items) {
        return new AmountIngredient(Ingredient.of(items), amount).toVanilla();
    }

    public static Ingredient of(int amount, ItemStack... stacks) {
        return new AmountIngredient(Ingredient.of(stacks), amount).toVanilla();
    }

    public static Ingredient of(int amount, TagKey<Item> tag) {
        return new AmountIngredient(Ingredient.of(tag), amount).toVanilla();
    }

    public static Ingredient of(ItemStack stack) {
        return new AmountIngredient(Ingredient.of(stack), stack.getCount()).toVanilla();
    }
}
