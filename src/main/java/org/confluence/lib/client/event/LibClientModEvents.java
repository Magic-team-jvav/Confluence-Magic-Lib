package org.confluence.lib.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterItemDecorationsEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.animation.third_person.AddPlayerGeoModelEvent;
import org.confluence.lib.api.animation.third_person.AnimationConstants;
import org.confluence.lib.api.animation.third_person.PlayerGeoAnimatable;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemExtension;
import org.confluence.lib.common.item.GroupItem;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.client.extensions.common.PortRegisterClientExtensionsEvent;

import java.util.concurrent.CompletableFuture;

public final class LibClientModEvents {
    public static void init() {
        PortEventHandler.addListener(LibClientModEvents::registerParticleProviders);
        PortEventHandler.addListener(LibClientModEvents::registerClientExtensions);
        PortEventHandler.addListener(LibClientModEvents::fmlClientSetup);
        PortEventHandler.addListener(LibClientModEvents::registerClientReloadListeners);
        PortEventHandler.addListener(LibClientModEvents::registerItemDecorations);
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
        if (AnimationConstants.SHOULD_APPLY) {
            event.enqueueWork(() -> PortEventHandler.postEvent(new AddPlayerGeoModelEvent()));
        }
    }

    private static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (AnimationConstants.SHOULD_APPLY) {
            event.registerReloadListener((pb, rm, pp, rp, be, ge) -> CompletableFuture.runAsync(() -> {
                for (Runnable callback : PlayerGeoAnimatable.reloadCallbacks) {
                    callback.run();
                }
            }, ge).thenCompose(pb::wait));
        }
    }

    public static void registerItemDecorations(RegisterItemDecorationsEvent event) {
        if (LibStartupConfig.itemGroups()) {
            ResourceLocation plus = ConfluenceMagicLib.asResource("plus");
            final ResourceLocation minus = ConfluenceMagicLib.asResource("minus");
            event.register(GroupItem.getInstance(), (guiGraphics, font, stack, xOffset, yOffset) -> {
                GroupItem.Stacks stacks = stack.get(ConfluenceMagicLib.GROUP_STACKS);
                if (stacks != null) {
                    PoseStack pose = guiGraphics.pose();
                    pose.pushPose();
                    pose.translate(xOffset + 9, yOffset + 9, 200);
                    guiGraphics.blitSprite(stacks.isVisible() ? minus : plus, 0, 0, 7, 7);
                    pose.popPose();
                }
                return false;
            });
        }
    }
}
