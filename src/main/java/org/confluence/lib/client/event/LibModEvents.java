package org.confluence.lib.client.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemSpecialRenderer;
import org.confluence.lib.client.render.item.properties.FunctionEnabled;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)
public final class LibModEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CrossDustParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderer(RegisterSpecialModelRendererEvent event) {
        if (LibStartupConfig.itemGroups()) {
            event.register(GroupItemSpecialRenderer.Unbaked.ID, GroupItemSpecialRenderer.Unbaked.MAP_CODEC);
        }
    }

    @SubscribeEvent
    public static void registerConditionalItemModelProperty(RegisterConditionalItemModelPropertyEvent event) {
        event.register(FunctionEnabled.ID, FunctionEnabled.CODEC);
    }
}
