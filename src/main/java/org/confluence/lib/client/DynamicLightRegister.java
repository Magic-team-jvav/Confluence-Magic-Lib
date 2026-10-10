package org.confluence.lib.client;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.confluence.lib.client.light.DynamicLightEffects;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;

@SuppressWarnings("unchecked")
public class DynamicLightRegister {
    private static final Map<EntityType<Entity>, DynamicLightProvider<Entity>> entityProviders = new HashMap<>();

    private static final Map<Class<?>, List<DynamicLightProvider<Entity>>> entityClassProviders = new HashMap<>();

    private static final Map<Class<?>, List<DynamicLightProvider<Entity>>> resolvedProviders = new HashMap<>();

    private static final Map<Class<Particle>, DynamicLightProvider<Particle>> particleTypeProviders = new HashMap<>();

    private static final Map<Item, Integer> items = new HashMap<>();

    private static final Map<Item, Integer> projectiles = new HashMap<>();

    private static final List<ItemRule> overrides = new ArrayList<>();

    private static final List<ToIntFunction<ItemStack>> fallbacks = new ArrayList<>();

    private static final List<FloatConsumer> collectors = new ArrayList<>();

    // 在 FMLClientSetupEvent 中注册

    public static <T extends Entity> void register(EntityType<T> type, DynamicLightProvider<T> dynamicLightProvider) {
        entityProviders.put((EntityType<Entity>) type, (DynamicLightProvider<Entity>) dynamicLightProvider);
    }

    public static <T extends Particle> void register(Class<T> type, DynamicLightProvider<T> dynamicLightProvider) {
        particleTypeProviders.put((Class<Particle>) type, (DynamicLightProvider<Particle>) dynamicLightProvider);
    }

    // 实体类规则匹配其子类；与按 EntityType 注册的规则共用 provider 采集逻辑。
    public static <T extends Entity> void registerEntity(Class<T> type, DynamicLightProvider<T> provider) {
        entityClassProviders.computeIfAbsent(type, ignored -> new ArrayList<>()).add((DynamicLightProvider<Entity>) provider);
        resolvedProviders.clear();
    }

    public static void registerItem(Item item, int brightness) {items.merge(item, brightness, Math::max);}

    public static void registerProjectile(Item item, int brightness) {projectiles.merge(item, brightness, Math::max);}

    // 特殊物品的开关规则优先于固定亮度和兜底规则，返回零即可关闭该物品的光源。
    public static void registerItemOverride(Class<? extends Item> type, ItemLightProvider provider) {
        overrides.add(new ItemRule(type, provider));
    }

    public static void registerItemFallback(ToIntFunction<ItemStack> provider) {fallbacks.add(provider);}

    public static void registerEquipment(EquipmentSlot... slots) {
        EquipmentSlot[] equipment = slots.clone();
        registerEntity(LivingEntity.class, DynamicLightProvider.entity(entity -> {
            int light = 0;
            for (EquipmentSlot slot : equipment)
                light = Math.max(light,
                        itemLuminance(entity.getItemBySlot(slot), entity, false));
            return light;
        }));
        registerEntity(ItemEntity.class, DynamicLightProvider.entity(entity -> itemLuminance(entity.getItem(), null, false)));
    }

    public static <T extends Entity> void registerProjectileItem(Class<T> type, Function<T, ItemStack> item) {
        registerEntity(type, DynamicLightProvider.entity(entity -> itemLuminance(item.apply(entity), null, true)));
    }

    public static void addCollector(FloatConsumer collector) {collectors.add(collector);}

    // 为非原版实体接入其已有类型和实例集合，仍在本帧快照生成前采集。
    public static <K, T> SourceGroup<K, T> registerGroup(SourceCollection<T> collection, Function<T, K> type) {
        SourceGroup<K, T> group = new SourceGroup<>(collection, type);
        addCollector(group::collect);
        return group;
    }

    public static void collect(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (FloatConsumer collector : collectors) collector.accept(partialTick);
        DynamicLightEffects.collect(partialTick);
        if (entityProviders.isEmpty() && entityClassProviders.isEmpty())
            return;
        // 第一人称也采集本地玩家，并避免在实体列表中重复处理。
        Entity player = Minecraft.getInstance().player;
        if (player != null) collectEntity(player, partialTick);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity != player) collectEntity(entity, partialTick);
        }
    }

    public static void renderParticle(Particle particle, float partialTick) {
        // 挂载点ParticleEngineMixin，未使用
        DynamicLightProvider<Particle> provider = particleTypeProviders.get(particle.getClass());
        if (provider != null) {
            DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(particle, partialTick));
        }
    }

    private static void collectEntity(Entity entity, float partialTick) {
        int brightness = collectProvider(entityProviders.get(entity.getType()), entity, partialTick);
        for (DynamicLightProvider<Entity> provider : providersFor(entity.getClass())) {
            brightness = Math.max(brightness, collectProvider(provider, entity, partialTick));
        }
        // 所有中心光源先取最大亮度，仅插值并创建一次光源；自定义位置的 provider 直接采集。
        if (brightness > 0) DynamicLightDispatcher.INSTANCE.addLightSource(
                DynamicLightProvider.EntityLight.source(entity, partialTick, brightness));
    }

    private static int collectProvider(DynamicLightProvider<Entity> provider, Entity entity, float partialTick) {
        if (provider instanceof DynamicLightProvider.EntityLight<?> centered) {
            return ((DynamicLightProvider.EntityLight<Entity>) centered).brightness(entity);
        }
        if (provider != null)
            DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(entity, partialTick));
        return 0;
    }

    private static List<DynamicLightProvider<Entity>> providersFor(Class<?> type) {
        return resolvedProviders.computeIfAbsent(type, concrete -> {
            List<DynamicLightProvider<Entity>> result = new ArrayList<>();
            entityClassProviders.forEach((registered, providers) -> {
                if (registered.isAssignableFrom(concrete)) result.addAll(providers);
            });
            return List.copyOf(result);
        });
    }

    private static int itemLuminance(ItemStack stack, @Nullable LivingEntity holder, boolean projectile) {
        if (stack.isEmpty()) return 0;
        for (ItemRule rule : overrides) {
            if (rule.type().isInstance(stack.getItem()))
                return rule.provider().luminance(stack, holder, projectile);
        }
        int light = (projectile ? projectiles : items).getOrDefault(stack.getItem(), 0);
        if (stack.getItem() instanceof BlockItem block)
            light = Math.max(light, blockLight(stack, block));
        for (ToIntFunction<ItemStack> provider : fallbacks)
            light = Math.max(light, provider.applyAsInt(stack));
        return light;
    }

    private static int blockLight(ItemStack stack, BlockItem block) {
        BlockState state = block.getBlock().defaultBlockState();
        var tag = stack.getTagElement("BlockStateTag");
        if (tag != null) {
            for (String name : tag.getAllKeys()) {
                Property<?> property = state.getBlock().getStateDefinition().getProperty(name);
                if (property != null) state = applyProperty(state, property, tag.getString(name));
            }
        }
        return state.getLightEmission();
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String value) {
        return property.getValue(value).map(parsed -> state.setValue(property, parsed)).orElse(state);
    }

    @FunctionalInterface
    public interface SourceCollection<T> {
        void forEach(float partialTick, Consumer<T> consumer);
    }

    @FunctionalInterface
    public interface ItemLightProvider {
        int luminance(ItemStack stack, @Nullable LivingEntity holder, boolean projectile);
    }

    private record ItemRule(Class<? extends Item> type, ItemLightProvider provider) {}

    public static final class SourceGroup<K, T> {
        private final SourceCollection<T> collection;
        private final Function<T, K> type;
        private final Map<K, DynamicLightProvider<T>> providers = new HashMap<>();
        private final Consumer<T> consumer = this::accept;
        private float partialTick;

        private SourceGroup(SourceCollection<T> collection, Function<T, K> type) {
            this.collection = collection;
            this.type = type;
        }

        public void register(K key, DynamicLightProvider<T> provider) {
            providers.put(key, provider);
        }

        private void collect(float partialTick) {
            this.partialTick = partialTick;
            collection.forEach(partialTick, consumer);
        }

        private void accept(T instance) {
            DynamicLightProvider<T> provider = providers.get(type.apply(instance));
            if (provider != null)
                DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(instance, partialTick));
        }
    }
}
