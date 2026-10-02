package org.confluence.lib.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.mesdag.portlib.event.client.PortRenderLevelStageEvent;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class DynamicLightRegister {

    private static final Map<EntityType<Entity>, DynamicLightProvider<Entity>> entityProviders = new HashMap<>();
    private static final Map<Class<Particle>, DynamicLightProvider<Particle>> particleTypeProviders = new HashMap<>();

    public static void renderEntity(PortRenderLevelStageEvent event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null && event.getStage().equals(PortRenderLevelStageEvent.Stage.AFTER_ENTITIES)) {
            Iterable<Entity> entities = level.entitiesForRendering();
            float partialTick = event.getPartialTick().getGameTimeDeltaPartialTick(true);
            for (Entity entity : entities) {
                DynamicLightProvider<Entity> provider = entityProviders.get(entity.getType());
                if (provider != null) {
                    DynamicLightDispatcher.INSTANCE.addLightSource(provider.getLuminance(entity, partialTick));
                }
            }
        }
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
