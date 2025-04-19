package org.confluence.lib;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.common.component.NbtComponent;
import org.confluence.lib.common.recipe.AmountIngredient;
import org.confluence.lib.common.worldgen.structure.GridPiece;
import org.confluence.lib.common.worldgen.structure.SimpleTemplatePiece;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

@Mod(ConfluenceMagicLib.LIB_ID)
public class ConfluenceMagicLib {
    public static final String LIB_ID = "confluence_magic_lib";
    public static final String CONFLUENCE_ID = "confluence";
    public static final Logger LOGGER = LoggerFactory.getLogger("Confluence Magic Lib");
    public static final boolean IS_CONFLUENCE_LOADED = ModList.get().isLoaded(CONFLUENCE_ID);

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, LIB_ID);
    public static final Supplier<IngredientType<AmountIngredient>> AMOUNT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("amount_ingredient", () -> new IngredientType<>(AmountIngredient.CODEC, AmountIngredient.STREAM_CODEC));

    public static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, LIB_ID);
    public static final Supplier<StructurePieceType.StructureTemplateType> SIMPLE_TEMPLATE_PIECE = PIECE_TYPES.register("simple_template_piece", () -> SimpleTemplatePiece::new);
    public static final Supplier<StructurePieceType.ContextlessType> GRID_PIECE = PIECE_TYPES.register("grid_piece", () -> GridPiece::new);

    public static final DeferredRegister.DataComponents TC_DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LIB_ID);
    public static final Supplier<DataComponentType<ModRarity>> MOD_RARITY = TC_DATA_COMPONENT_TYPES.registerComponentType("mod_rarity", builder -> builder.persistent(ModRarity.CODEC).networkSynchronized(ModRarity.STREAM_CODEC));
    public static final Supplier<DataComponentType<NbtComponent>> NBT = TC_DATA_COMPONENT_TYPES.registerComponentType("nbt", builder -> builder.persistent(NbtComponent.CODEC).networkSynchronized(NbtComponent.STREAM_CODEC));

    public ConfluenceMagicLib(IEventBus modEventBus, ModContainer modContainer) {
        INGREDIENT_TYPES.register(modEventBus);
        PIECE_TYPES.register(modEventBus);
        TC_DATA_COMPONENT_TYPES.register(modEventBus);
    }

    public static ResourceLocation lib(String path) {
        return ResourceLocation.fromNamespaceAndPath(LIB_ID, path);
    }

    public static ResourceLocation confluence(String path) {
        return ResourceLocation.fromNamespaceAndPath(CONFLUENCE_ID, path);
    }
}
