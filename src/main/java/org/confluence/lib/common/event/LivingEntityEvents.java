package org.confluence.lib.common.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.util.LibUtils;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, modid = ConfluenceMagicLib.MODID)
public final class LivingEntityEvents {
    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(LibUtils.NO_DROPS_TAG)) {
            event.setCanceled(true);
        }
    }
}
