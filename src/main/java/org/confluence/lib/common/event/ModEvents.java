package org.confluence.lib.common.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.fluid.FluidBuilder;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModEvents {
    @SubscribeEvent
    public static void register(RegisterEvent event) {
        FluidBuilder.register(event);
    }
}
