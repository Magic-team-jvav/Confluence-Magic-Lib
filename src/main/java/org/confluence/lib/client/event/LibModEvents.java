package org.confluence.lib.client.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemExtension;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.integration.animation.AddPlayerGeoModelEvent;
import org.confluence.lib.integration.animation.AnimationConstants;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)
public final class LibModEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CrossDustParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        if (LibStartupConfig.itemGroups()) {
            event.registerItem(GroupItemExtension.INSTANCE, GroupItem.getInstance());
        }
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (AnimationConstants.SHOULD_APPLY) {
                ModLoader.postEvent(new AddPlayerGeoModelEvent());
            }
        });
    }
}
