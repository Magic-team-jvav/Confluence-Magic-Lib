package org.confluence.lib.api.event;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

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
