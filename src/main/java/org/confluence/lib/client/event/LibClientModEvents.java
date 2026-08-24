package org.confluence.lib.client.event;

import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemExtension;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.integration.animation.AddPlayerGeoModelEvent;
import org.confluence.lib.integration.animation.AnimationConstants;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.client.extensions.common.PortRegisterClientExtensionsEvent;

public final class LibClientModEvents {
    public static void init() {
        PortEventHandler.addListener(LibClientModEvents::registerParticleProviders);
        PortEventHandler.addListener(LibClientModEvents::registerClientExtensions);
        PortEventHandler.addListener(LibClientModEvents::fmlClientSetup);
    }

    private static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CrossDustParticle.Provider::new);
    }

    private static void registerClientExtensions(PortRegisterClientExtensionsEvent event) {
        if (LibStartupConfig.itemGroups()) {
            event.registerItem(GroupItemExtension.INSTANCE, GroupItem.getInstance());
        }
    }

    private static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (AnimationConstants.SHOULD_APPLY) {
                PortEventHandler.postEvent(new AddPlayerGeoModelEvent());
            }
        });
    }
}
