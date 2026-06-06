package org.confluence.lib;

import com.mojang.datafixers.util.Function4;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.common.component.NbtComponent;
import org.confluence.lib.common.component.ToolMode;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.common.particle.CrossDustParticleOptions;
import org.confluence.lib.common.recipe.AmountIngredient;
import org.confluence.lib.common.worldgen.structure.GridPiece;
import org.confluence.lib.common.worldgen.structure.SimpleTemplatePiece;
import org.confluence.lib.network.AttackDamagePacketS2C;
import org.confluence.lib.network.SetEntityDataPacketS2C;
import org.confluence.lib.util.DelayTaskHolder;
import org.confluence.lib.util.LibUtils;
import org.mesdag.portlib.attachment.PortAttachmentType;
import org.mesdag.portlib.component.PortDataComponentType;
import org.mesdag.portlib.network.PortNetworkHandler;
import org.mesdag.portlib.registries.*;
import org.mesdag.portlib.wrapper.common.PortPercentageAttribute;
import org.mesdag.portlib.wrapper.common.crafting.PortIngredientType;
import org.mesdag.portlib.wrapper.world.entity.ai.attributes.PortAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

@Mod(ConfluenceMagicLib.LIB_ID)
public final class ConfluenceMagicLib {
    public static final String LIB_ID = "confluence_magic_lib";
    public static final String CONFLUENCE_ID = "confluence";
    public static final Logger LOGGER = LoggerFactory.getLogger("Confluence Magic Lib");
    public static final PortNetworkHandler NETWORK_HANDLER = new PortNetworkHandler(LIB_ID, "1");
    public static final boolean IS_CONFLUENCE_LOAD = LibUtils.isModLoaded(CONFLUENCE_ID);

    private static final PortRegistration<Item> ITEMS = PortRegisterHandler.create(LIB_ID, Registries.ITEM);
    public static final PortRegistryEntry<Item, GroupItem> GROUP_ITEM;

    static {
        GROUP_ITEM = ITEMS.register("group", GroupItem::new);
    }

    private static final PortAttributeRegistration ATTRIBUTES = PortRegisterHandler.attribute(LIB_ID);
    public static final PortRegistryEntry<Attribute, PortPercentageAttribute> CRITICAL_CHANCE = registerAttribute("generic.critical_chance", 0.0, 0.0, 10.0, PortPercentageAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> RANGED_VELOCITY = registerAttribute("generic.ranged_velocity", 1.0, 0.0, 10.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> RANGED_DAMAGE = registerAttribute("generic.ranged_damage", 1.0, 0.0, 10.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, PortPercentageAttribute> DODGE_CHANCE = registerAttribute("generic.dodge_chance", 0.0, 0.0, 1.0, PortPercentageAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> MAGIC_DAMAGE = registerAttribute("generic.magic_damage", 1.0, 0.0, 10.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> ARMOR_PENETRATION = registerAttribute("generic.armor_penetration", 0.0, 0.0, 10000, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> MOB_SPAWN_SPEED_MULTIPLIER = registerAttribute("player.mob_spawn_speed_multiplier", 1, 0, 1024, RangedAttribute::new, maker -> maker.setSentiment(PortAttribute.PortSentiment.NEUTRAL));
    public static final PortRegistryEntry<Attribute, RangedAttribute> MOB_SPAWN_COUNT_MULTIPLIER = registerAttribute("player.mob_spawn_count_multiplier", 1, 0, 1024, RangedAttribute::new, maker -> maker.setSentiment(PortAttribute.PortSentiment.NEUTRAL));
    public static final PortRegistryEntry<Attribute, RangedAttribute> PICKUP_RANGE = registerAttribute("player.pickup_range", 0.0, 0.0, 64.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> AGGRO = registerAttribute("player.aggro", 0.0, -10000.0, 10000.0, RangedAttribute::new, maker -> maker.setSentiment(PortAttribute.PortSentiment.NEGATIVE));
    public static final PortRegistryEntry<Attribute, RangedAttribute> MINION_CAPACITY = registerAttribute("player.minion_capacity", 1.0, 0.0, 128.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> SENTRY_CAPACITY = registerAttribute("player.sentry_capacity", 1.0, 0.0, 128.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> SUMMON_DAMAGE = registerAttribute("player.summon_damage", 1.0, 0.0, 2048.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> SUMMON_KNOCKBACK = registerAttribute("player.summon_knockback", 0.0, 0.0, 5.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> WHIP_RANGE = registerAttribute("player.whip_range", 3.0, 0.0, 64.0, RangedAttribute::new, maker -> maker.setSyncable(true));
    public static final PortRegistryEntry<Attribute, RangedAttribute> MARK_DAMAGE = registerAttribute("player.mark_damage", 0.0, 0.0, 1024.0, RangedAttribute::new, maker -> maker.setSyncable(true));

    private static <A extends Attribute> PortRegistryEntry<Attribute, A> registerAttribute(String name, double defaultValue, double min, double max, Function4<String, Double, Double, Double, A> factory, Consumer<PortAttributeRegistration.PortAttributeMaker> consumer) {
        return ATTRIBUTES.register(name, () -> factory.apply("attribute.name." + name, defaultValue, min, max), consumer);
    }

    private static final PortIngredientTypeRegistration INGREDIENT_TYPES = PortRegisterHandler.ingredientType(LIB_ID);
    public static final PortRegistryEntry<PortIngredientType<?>, PortIngredientType<AmountIngredient>> AMOUNT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("amount_ingredient", () -> new PortIngredientType<>(AmountIngredient.CODEC, AmountIngredient.STREAM_CODEC));

    private static final PortRegistration<StructurePieceType> PIECE_TYPES = PortRegisterHandler.create(LIB_ID, Registries.STRUCTURE_PIECE);
    public static final PortRegistryEntry<StructurePieceType, StructurePieceType.StructureTemplateType> SIMPLE_TEMPLATE_PIECE = PIECE_TYPES.register("simple_template_piece", () -> SimpleTemplatePiece::new);
    public static final PortRegistryEntry<StructurePieceType, StructurePieceType.ContextlessType> GRID_PIECE = PIECE_TYPES.register("grid_piece", () -> GridPiece::new);

    private static final PortDataComponentRegistration DATA_COMPONENTS = PortRegisterHandler.dataComponent(LIB_ID);
    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<ModRarity>> MOD_RARITY = DATA_COMPONENTS.builder("mod_rarity", b -> b.persistent(ModRarity.CODEC).networkSynchronized(ModRarity.STREAM_CODEC));
    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<ToolMode>> TOOL_MODE = DATA_COMPONENTS.builder("tool_mode", b -> b.persistent(ToolMode.CODEC).networkSynchronized(ToolMode.STREAM_CODEC));
    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<NbtComponent>> NBT = DATA_COMPONENTS.builder("nbt", b -> b.persistent(NbtComponent.CODEC).networkSynchronized(NbtComponent.STREAM_CODEC));
    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<GroupItem.Stacks>> GROUP_STACKS = DATA_COMPONENTS.builder("group_stacks", b -> b.persistent(GroupItem.Stacks.CODEC).networkSynchronized(GroupItem.Stacks.STREAM_CODEC));
    public static final PortRegistryEntry<PortDataComponentType<?>, PortDataComponentType<GroupItem.BelongsTo>> BELONGS_TO_GROUP = DATA_COMPONENTS.builder("belongs_to_group", b -> b.persistent(GroupItem.BelongsTo.CODEC).networkSynchronized(GroupItem.BelongsTo.STREAM_CODEC));

    private static final PortParticleTypeRegistration PARTICLES = PortRegisterHandler.particleType(LIB_ID);
    public static final PortRegistryEntry<ParticleType<?>, ParticleType<CrossDustParticleOptions>> CROSS_DUST_PARTICLE = PARTICLES.register("cross_dust", false, CrossDustParticleOptions.CODEC, CrossDustParticleOptions.STREAM_CODEC);

    private static final PortAttachmentRegistration ATTACHMENTS = PortRegisterHandler.attachment(LIB_ID);
    public static final PortRegistryEntry<PortAttachmentType<?>, PortAttachmentType<DelayTaskHolder>> DELAY_TASK_HOLDER = ATTACHMENTS.registerSimple("delay_task_holder", () -> PortAttachmentType.builder(DelayTaskHolder::new));

    public ConfluenceMagicLib(FMLJavaModLoadingContext context) {
        LibStartupConfig.register();

        NETWORK_HANDLER.registerInGameS2C(AttackDamagePacketS2C.class, AttackDamagePacketS2C.ID, AttackDamagePacketS2C.STREAM_CODEC);
        NETWORK_HANDLER.registerInGameS2C(SetEntityDataPacketS2C.class, SetEntityDataPacketS2C.ID, SetEntityDataPacketS2C.STREAM_CODEC);

        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.crit_chance"), CRITICAL_CHANCE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.ranged_velocity"), RANGED_VELOCITY.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.ranged_damage"), RANGED_DAMAGE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.dodge_chance"), DODGE_CHANCE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.magic_damage"), MAGIC_DAMAGE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "generic.armor_penetration"), ARMOR_PENETRATION.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "player.pickup_range"), PICKUP_RANGE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_curio", "player.aggro"), AGGRO.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.minion_capacity"), MINION_CAPACITY.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.sentry_capacity"), SENTRY_CAPACITY.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.summon_damage"), SUMMON_DAMAGE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.summon_knockback"), SUMMON_KNOCKBACK.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.whip_range"), WHIP_RANGE.getId());
        ATTRIBUTES.addAlias(ResourceLocation.fromNamespaceAndPath("terra_entity", "player.mark_damage"), MARK_DAMAGE.getId());

        org.confluence.lib.common.event.LibModEvents.init();
        org.confluence.lib.common.event.LibGameEvents.init();
        if (LibUtils.isPhysicalClient()) {
            org.confluence.lib.client.event.LibModEvents.init();
            org.confluence.lib.client.event.LibGameEvents.init();
        }
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(LIB_ID, path);
    }

    public static ResourceLocation asConfluenceResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(CONFLUENCE_ID, path);
    }
}
