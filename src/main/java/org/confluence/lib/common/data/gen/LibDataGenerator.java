package org.confluence.lib.common.data.gen;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class LibDataGenerator {
    @SubscribeEvent
    public static void gatherData$Client(GatherDataEvent.Client event) {
        event.createProvider(LibDamageTypeTagsProvider::new);
        event.createProvider(LibEntityTypeTagsProvider::new);
        event.createProvider(LibItemTagsProvider::new);
    }
}
