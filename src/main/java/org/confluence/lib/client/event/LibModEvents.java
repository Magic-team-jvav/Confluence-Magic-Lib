package org.confluence.lib.client.event;

import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemExtension;
import org.confluence.lib.common.item.GroupItem;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.client.PortRegisterParticleProvidersEvent;
import org.mesdag.portlib.event.client.extensions.common.PortRegisterClientExtensionsEvent;

public final class LibModEvents {
    public static void init() {
        PortEventHandler.addListener(LibModEvents::registerParticleProviders);
        PortEventHandler.addListener(LibModEvents::registerClientExtensions);
    }

    private static void registerParticleProviders(PortRegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CrossDustParticle.Provider::new);
    }

    private static void registerClientExtensions(PortRegisterClientExtensionsEvent event) {
        if (LibStartupConfig.itemGroups()) {
            event.registerItem(GroupItemExtension.INSTANCE, GroupItem.getInstance());
        }
    }
}
