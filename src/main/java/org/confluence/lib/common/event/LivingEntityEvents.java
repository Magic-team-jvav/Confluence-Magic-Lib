package org.confluence.lib.common.event;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.util.LibUtils;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = ConfluenceMagicLib.LIB_ID)
public final class LivingEntityEvents {
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
}
