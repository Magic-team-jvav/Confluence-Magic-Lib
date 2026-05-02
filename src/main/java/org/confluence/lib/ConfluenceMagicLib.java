package org.confluence.lib;

import com.mojang.datafixers.util.Function4;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.PercentageAttribute;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.common.component.NbtComponent;
import org.confluence.lib.common.component.ToolMode;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.common.particle.CrossDustParticleOptions;
import org.confluence.lib.common.recipe.AmountIngredient;
import org.confluence.lib.common.worldgen.structure.GridPiece;
import org.confluence.lib.common.worldgen.structure.SimpleTemplatePiece;
import org.confluence.lib.util.DelayTaskHolder;
import org.confluence.lib.util.LibUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Mod(ConfluenceMagicLib.LIB_ID)
public final class ConfluenceMagicLib {
    public static final String LIB_ID = "confluence_magic_lib";
    public static final String CONFLUENCE_ID = "confluence";
    public static final Logger LOGGER = LoggerFactory.getLogger("Confluence Magic Lib");
    public static final boolean IS_CONFLUENCE_LOAD = LibUtils.isModLoaded(CONFLUENCE_ID);
    @Deprecated
    public static final Supplier<Boolean> IS_CONFLUENCE_LOADED = () -> IS_CONFLUENCE_LOAD;

    // region 物品
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LIB_ID);

    private static final DeferredItem<GroupItem> GROUP = ITEMS.register("group", GroupItem::new);
    // endregion

    // region 数据附件
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPE = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LIB_ID);

    public static final Supplier<AttachmentType<DelayTaskHolder>> DELAY_TASK_HOLDER = ATTACHMENT_TYPE.register("delay_task_holder", () -> AttachmentType.builder(DelayTaskHolder::new).build());
    // endregion

    // region 属性
    private static final DeferredRegister<Attribute> ATTRIBUTES = DeferredRegister.create(Registries.ATTRIBUTE, LIB_ID);
    /// 暴击率，请优先使用[org.confluence.lib.common.LibAttributes#getCriticalChance]
    public static final DeferredHolder<Attribute, PercentageAttribute> CRITICAL_CHANCE = registerAttribute("generic.critical_chance", 0.0, 0.0, 10.0, PercentageAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 远程速度，请优先使用[org.confluence.lib.common.LibAttributes#getRangedVelocity]
    public static final DeferredHolder<Attribute, RangedAttribute> RANGED_VELOCITY = registerAttribute("generic.ranged_velocity", 1.0, 0.0, 10.0, RangedAttribute::new, a -> a.setSyncable(true)); // MULTIPLY_TOTAL
    /// 远程伤害，请优先使用[org.confluence.lib.common.LibAttributes#getRangedDamage]
    public static final DeferredHolder<Attribute, RangedAttribute> RANGED_DAMAGE = registerAttribute("generic.ranged_damage", 1.0, 0.0, 10.0, RangedAttribute::new, a -> a.setSyncable(true)); // MULTIPLY_TOTAL
    /// 闪避率，请优先使用[org.confluence.lib.common.LibAttributes#getDodgeChance]
    public static final DeferredHolder<Attribute, PercentageAttribute> DODGE_CHANCE = registerAttribute("generic.dodge_chance", 0.0, 0.0, 1.0, PercentageAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 魔法伤害，请优先使用[org.confluence.lib.common.LibAttributes#getMagicDamage]
    public static final DeferredHolder<Attribute, RangedAttribute> MAGIC_DAMAGE = registerAttribute("generic.magic_damage", 1.0, 0.0, 10.0, RangedAttribute::new, a -> a.setSyncable(true)); // MULTIPLY_TOTAL
    /// 护甲穿透，请优先使用[org.confluence.lib.common.LibAttributes#getArmorPenetration]
    public static final DeferredHolder<Attribute, RangedAttribute> ARMOR_PENETRATION = registerAttribute("generic.armor_penetration", 0.0, 0.0, 10000, RangedAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 生物生成速度系数
    public static final DeferredHolder<Attribute, RangedAttribute> MOB_SPAWN_SPEED_MULTIPLIER = registerAttribute("player.mob_spawn_speed_multiplier", 1, 0, 1024, RangedAttribute::new, a -> a.setSentiment(Attribute.Sentiment.NEUTRAL));
    /// 生物生成数量系数
    public static final DeferredHolder<Attribute, RangedAttribute> MOB_SPAWN_COUNT_MULTIPLIER = registerAttribute("player.mob_spawn_count_multiplier", 1, 0, 1024, RangedAttribute::new, a -> a.setSentiment(Attribute.Sentiment.NEUTRAL));
    /// 拾取范围
    public static final DeferredHolder<Attribute, RangedAttribute> PICKUP_RANGE = registerAttribute("player.pickup_range", 0.0, 0.0, 64.0, RangedAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 仇恨值
    public static final DeferredHolder<Attribute, RangedAttribute> AGGRO = registerAttribute("player.aggro", 0.0, -10000.0, 10000.0, RangedAttribute::new, a -> a.setSentiment(Attribute.Sentiment.NEGATIVE)); // ADD_VALUE
    /// 仆从容量
    public static final DeferredHolder<Attribute, RangedAttribute> MINION_CAPACITY = registerAttribute("player.minion_capacity", 1.0, 0.0, 128.0, RangedAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 哨兵容量
    public static final DeferredHolder<Attribute, RangedAttribute> SENTRY_CAPACITY = registerAttribute("player.sentry_capacity", 1.0, 0.0, 128.0, RangedAttribute::new, a -> a.setSyncable(true)); // ADD_VALUE
    /// 召唤伤害，请优先使用[org.confluence.lib.common.LibAttributes#getSummonDamage]
    public static final DeferredHolder<Attribute, RangedAttribute> SUMMON_DAMAGE = registerAttribute("player.summon_damage", 1.0, 0.0, 2048.0, RangedAttribute::new, a -> a.setSyncable(true)); // MULTIPLY_TOTAL
    /// 仆从击退
    public static final DeferredHolder<Attribute, RangedAttribute> SUMMON_KNOCKBACK = registerAttribute("player.summon_knockback", 0.0, 0.0, 5.0, RangedAttribute::new, a -> a.setSyncable(true));
    // 鞭速度 同 近战攻击速度，故不注册
    /// 鞭范围
    public static final DeferredHolder<Attribute, RangedAttribute> WHIP_RANGE = registerAttribute("player.whip_range", 3.0, 0.0, 64.0, RangedAttribute::new, a -> a.setSyncable(true));
    /// 仆从标记伤害
    public static final DeferredHolder<Attribute, RangedAttribute> MARK_DAMAGE = registerAttribute("player.mark_damage", 0.0, 0.0, 1024.0, RangedAttribute::new, a -> a.setSyncable(true));

    private static <A extends Attribute> DeferredHolder<Attribute, A> registerAttribute(String name, double defaultValue, double min, double max, Function4<String, Double, Double, Double, A> factory, Consumer<A> consumer) {
        return ATTRIBUTES.register(name, () -> {
            A a = factory.apply("attribute.name." + name, defaultValue, min, max);
            consumer.accept(a);
            return a;
        });
    }
    // endregion

    // region 材料类型
    private static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, LIB_ID);
    public static final Supplier<IngredientType<AmountIngredient>> AMOUNT_INGREDIENT_TYPE = INGREDIENT_TYPES.register("amount_ingredient", () -> new IngredientType<>(AmountIngredient.CODEC, AmountIngredient.STREAM_CODEC));
    // endregion

    // region 结构
    private static final DeferredRegister<StructurePieceType> PIECE_TYPES = DeferredRegister.create(BuiltInRegistries.STRUCTURE_PIECE, LIB_ID);
    public static final Supplier<StructurePieceType.StructureTemplateType> SIMPLE_TEMPLATE_PIECE = PIECE_TYPES.register("simple_template_piece", () -> SimpleTemplatePiece::new);
    public static final Supplier<StructurePieceType.ContextlessType> GRID_PIECE = PIECE_TYPES.register("grid_piece", () -> GridPiece::new);
    // endregion

    // region 数据组件
    private static final DeferredRegister.DataComponents DATA_COMPONENT_TYPES = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LIB_ID);
    public static final Supplier<DataComponentType<ModRarity>> MOD_RARITY = DATA_COMPONENT_TYPES.registerComponentType("mod_rarity", builder -> builder.persistent(ModRarity.CODEC).networkSynchronized(ModRarity.STREAM_CODEC));
    public static final Supplier<DataComponentType<ToolMode>> TOOL_MODE = DATA_COMPONENT_TYPES.registerComponentType("tool_mode", builder -> builder.persistent(ToolMode.CODEC).networkSynchronized(ToolMode.STREAM_CODEC));
    public static final Supplier<DataComponentType<NbtComponent>> NBT = DATA_COMPONENT_TYPES.registerComponentType("nbt", builder -> builder.persistent(NbtComponent.CODEC).networkSynchronized(NbtComponent.STREAM_CODEC));
    public static final Supplier<DataComponentType<GroupItem.Stacks>> GROUP_STACKS = DATA_COMPONENT_TYPES.registerComponentType("group_stacks", builder -> builder.persistent(GroupItem.Stacks.CODEC).networkSynchronized(GroupItem.Stacks.STREAM_CODEC));
    public static final Supplier<DataComponentType<GroupItem.BelongsTo>> BELONGS_TO_GROUP = DATA_COMPONENT_TYPES.registerComponentType("belongs_to_group", builder -> builder.persistent(GroupItem.BelongsTo.CODEC).networkSynchronized(GroupItem.BelongsTo.STREAM_CODEC));
    // endregion

    // region 粒子
    private static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, LIB_ID);
    public static final Supplier<ParticleType<CrossDustParticleOptions>> CROSS_DUST_PARTICLE = PARTICLES.register("cross_dust", () -> new ParticleType<>(false) {
        @Override
        @NotNull
        public MapCodec<CrossDustParticleOptions> codec() {
            return CrossDustParticleOptions.CODEC;
        }

        @Override
        @NotNull
        public StreamCodec<? super RegistryFriendlyByteBuf, CrossDustParticleOptions> streamCodec() {
            return CrossDustParticleOptions.STREAM_CODEC;
        }
    });
    // endregion

    public ConfluenceMagicLib(IEventBus eventBus, ModContainer container) {
        LibStartupConfig.register(container);
        ITEMS.register(eventBus);
        ATTACHMENT_TYPE.register(eventBus);
        {
            ATTRIBUTES.register(eventBus);
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
        }
        INGREDIENT_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
        DATA_COMPONENT_TYPES.register(eventBus);
        PARTICLES.register(eventBus);
    }

    public static ResourceLocation asResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(LIB_ID, path);
    }

    public static ResourceLocation asConfluenceResource(String path) {
        return ResourceLocation.fromNamespaceAndPath(CONFLUENCE_ID, path);
    }
}
