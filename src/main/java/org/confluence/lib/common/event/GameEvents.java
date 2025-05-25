package org.confluence.lib.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.data.IdFixer;
import org.confluence.lib.common.data.saved.IGlobalData;
import org.confluence.lib.mixed.IExtraSyncedData;
import org.confluence.lib.network.SetEntityDataPacketS2C;
import org.confluence.lib.util.LibUtils;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = ConfluenceMagicLib.LIB_ID)
public final class GameEvents {
    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(LibUtils.NO_DROPS_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().getActiveEffects().stream().anyMatch(instance -> instance.getCures().contains(LibUtils.DENY_HEAL))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof IExtraSyncedData<?> extraSyncedData) {
            PacketDistributor.sendToPlayer(serverPlayer, new SetEntityDataPacketS2C(
                    extraSyncedData.confluence$self().getId(),
                    extraSyncedData.confluence$getAllEntries()
            ));
        }
    }

    @SubscribeEvent
    public static void serverStop(ServerStoppedEvent event) {
        for (IGlobalData data : IGlobalData.DAT) {
            data.clear();
        }
    }

    @SubscribeEvent
    public static void playerLogged(PlayerEvent.PlayerLoggedInEvent event) {
        IdFixer.fixPersistentData(event.getEntity());
    }
}
