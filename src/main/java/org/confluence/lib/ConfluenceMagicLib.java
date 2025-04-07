package org.confluence.lib;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.confluence.lib.recipe.AmountIngredient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Mod(ConfluenceMagicLib.MODID)
public class ConfluenceMagicLib {
    public static final String MODID = "confluence_magic_lib";
    public static final Logger LOGGER = LoggerFactory.getLogger("Confluence Magic Lib");

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, MODID);
    public static final Supplier<IngredientType<AmountIngredient>> AMOUNT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("amount_ingredient", () -> new IngredientType<>(AmountIngredient.CODEC, AmountIngredient.STREAM_CODEC));


    public ConfluenceMagicLib(IEventBus modEventBus, ModContainer modContainer) {
        INGREDIENT_TYPES.register(modEventBus);
    }
}
