package org.confluence.lib.client;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.light.DynamicItemLights;
import org.confluence.lib.client.light.DynamicLightEffects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

@SuppressWarnings("unchecked")
public class DynamicLightRegister {

    private static final Map<Class<?>, List<ToIntFunction<Entity>>> luminanceProviders = new HashMap<>();
    private static final Map<Class<?>, List<ToIntFunction<Entity>>> resolvedProviders = new HashMap<>();
    private static final Map<EntityType<?>, Integer> entityLuminance = new HashMap<>();

    // 亮度规则返回原版 0～15 等级，由采集器统一插值位置并转换为内部强度。
    public static <T extends Entity> void registerEntityLuminance(Class<T> type, ToIntFunction<T> provider) {
        luminanceProviders.computeIfAbsent(type, ignored -> new ArrayList<>()).add(entity -> provider.applyAsInt(type.cast(entity)));
        resolvedProviders.clear();
    }

    public static <T extends Entity> void registerProjectileItem(Class<T> type, Function<T, ItemStack> item) {
        registerEntityLuminance(type, entity -> DynamicItemLights.luminance(item.apply(entity), null, true));
    }

    public static void registerEquipment(EquipmentSlot... slots) {
        EquipmentSlot[] equipment = slots.clone();
        registerEntityLuminance(LivingEntity.class, entity -> {
            int light = 0;
            for (EquipmentSlot slot : equipment)
                light = Math.max(light,
                        DynamicItemLights.luminance(entity.getItemBySlot(slot), entity, false));
            return light;
        });
        registerEntityLuminance(ItemEntity.class, entity -> DynamicItemLights.luminance(entity.getItem(), null, false));
    }

    private static List<ToIntFunction<Entity>> providersFor(Class<?> type) {
        return resolvedProviders.computeIfAbsent(type, concrete -> {
            List<ToIntFunction<Entity>> result = new ArrayList<>();
            luminanceProviders.forEach((registered, providers) -> {
                if (registered.isAssignableFrom(concrete)) result.addAll(providers);
            });
            return List.copyOf(result);
        });
    }

    public static void registerEntityLuminance(EntityType<?> type, int luminance) {
        entityLuminance.merge(type, Mth.clamp(luminance, 0, 15), Math::max);
    }

    private static final List<FloatConsumer> collectors = new ArrayList<>();

    public static void addCollector(FloatConsumer collector) {collectors.add(collector);}

    private static final Map<EntityType<Entity>, DynamicLightProvider<Entity>> entityProviders = new HashMap<>();
    private static final Map<Class<Particle>, DynamicLightProvider<Particle>> particleTypeProviders = new HashMap<>();

    public static void collect(float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) return;
        for (FloatConsumer collector : collectors) collector.accept(partialTick);
        DynamicLightEffects.collect(partialTick);
        if (entityProviders.isEmpty() && entityLuminance.isEmpty() && luminanceProviders.isEmpty())
            return;
        // 第一人称也采集本地玩家，并避免在实体列表中重复处理。
        Entity player = Minecraft.getInstance().player;
        if (player != null) collectEntity(player, partialTick);
        for (Entity entity : level.entitiesForRendering()) {
            if (entity != player) collectEntity(entity, partialTick);
        }
    }

    private static void collectEntity(Entity entity, float partialTick) {
        DynamicLightProvider<Entity> provider = entityProviders.get(entity.getType());
        if (provider != null)
            DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(entity, partialTick));
        int luminance = entityLuminance.getOrDefault(entity.getType(), 0);
        for (ToIntFunction<Entity> rule : providersFor(entity.getClass()))
            luminance = Math.max(luminance, rule.applyAsInt(entity));
        if (luminance <= 0) return;
        Vec3 position = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.5,
                Mth.lerp(partialTick, entity.zo, entity.getZ()));
        DynamicLightDispatcher.INSTANCE.addLightSource(position, Mth.clamp(luminance, 0, 15) * 17);
    }

    public static void renderParticle(Particle particle, float partialTick) {
        // 挂载点ParticleEngineMixin，未使用
        DynamicLightProvider<Particle> provider = particleTypeProviders.get(particle.getClass());
        if (provider != null) {
            DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(particle, partialTick));
        }
    }

    // 在 FMLClientSetupEvent 中注册

    public static <T extends Entity> void register(EntityType<T> type, DynamicLightProvider<T> dynamicLightProvider) {
        entityProviders.put((EntityType<Entity>) type, (DynamicLightProvider<Entity>) dynamicLightProvider);
    }

    public static <T extends Particle> void register(Class<T> type, DynamicLightProvider<T> dynamicLightProvider) {
        particleTypeProviders.put((Class<Particle>) type, (DynamicLightProvider<Particle>) dynamicLightProvider);
    }
}
