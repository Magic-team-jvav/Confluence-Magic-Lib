package org.confluence.lib.api.event;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.ApiStatus;

import java.util.Map;

public abstract class NameFixRegisterEvent extends Event implements IModBusEvent {
    private ImmutableMap.Builder<ResourceLocation, ResourceLocation> builder;

    protected NameFixRegisterEvent() {}

    @ApiStatus.Internal
    public Map<ResourceLocation, ResourceLocation> getAlias() {
        return builder == null ? Map.of() : builder.build();
    }

    public NameFixRegisterEvent register(String from, String to) {
        return register(ResourceLocation.parse(from), ResourceLocation.parse(to));
    }

    public NameFixRegisterEvent register(ResourceLocation from, ResourceLocation to) {
        if (builder == null) builder = ImmutableMap.builder();
        builder.put(from, to);
        return this;
    }

    public NameFixRegisterEvent register(String from, ResourceLocation to) {
        return register(ResourceLocation.parse(from), to);
    }

    public NameFixRegisterEvent register(ResourceLocation from, String to) {
        return register(from, ResourceLocation.parse(to));
    }

    public static class BlockWithItem extends NameFixRegisterEvent {
        @ApiStatus.Internal
        public BlockWithItem() {}
    }

    public static class Block extends NameFixRegisterEvent {
        @ApiStatus.Internal
        public Block() {}
    }

    public static class Item extends NameFixRegisterEvent {
        @ApiStatus.Internal
        public Item() {}
    }

    public static class Data extends NameFixRegisterEvent {
        private final ResourceKey<? extends Registry<?>> registryKey;

        @ApiStatus.Internal
        public Data(ResourceKey<? extends Registry<?>> registryKey) {
            this.registryKey = registryKey;
        }

        public ResourceKey<? extends Registry<?>> getRegistryKey() {
            return registryKey;
        }
    }
}
