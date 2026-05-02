package org.confluence.lib.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;

import java.util.function.Consumer;

public class AddGroupInvalidCreativeModeTabEvent extends Event implements IModBusEvent {
    private final Consumer<ResourceKey<CreativeModeTab>> consumer;

    public AddGroupInvalidCreativeModeTabEvent(Consumer<ResourceKey<CreativeModeTab>> consumer) {
        this.consumer = consumer;
    }

    public void add(ResourceKey<CreativeModeTab> tabKey) {
        consumer.accept(tabKey);
    }
}
