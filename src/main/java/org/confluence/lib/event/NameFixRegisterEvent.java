package org.confluence.lib.event;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public abstract class NameFixRegisterEvent extends Event implements IModBusEvent {
    private final org.confluence.lib.api.event.NameFixRegisterEvent e;

    public NameFixRegisterEvent(org.confluence.lib.api.event.NameFixRegisterEvent e) {
        this.e = e;
    }

    public NameFixRegisterEvent register(String source, String target) {
        e.register(source, target);
        return this;
    }

    public NameFixRegisterEvent register(ResourceLocation source, ResourceLocation target) {
        e.register(source, target);
        return this;
    }

    public NameFixRegisterEvent register(String source, ResourceLocation target) {
        e.register(source, target);
        return this;
    }

    public NameFixRegisterEvent register(ResourceLocation source, String target) {
        e.register(source, target);
        return this;
    }

    public static class BlockWithItem extends NameFixRegisterEvent {
        public BlockWithItem(org.confluence.lib.api.event.NameFixRegisterEvent.BlockWithItem e) {
            super(e);
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.NameFixRegisterEvent.BlockWithItem.class, e -> NeoForge.EVENT_BUS.post(new BlockWithItem(e)));
        }
    }

    public static class Block extends NameFixRegisterEvent {
        public Block(org.confluence.lib.api.event.NameFixRegisterEvent.Block e) {
            super(e);
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.NameFixRegisterEvent.Block.class, e -> NeoForge.EVENT_BUS.post(new Block(e)));
        }
    }

    public static class Item extends NameFixRegisterEvent {
        public Item(org.confluence.lib.api.event.NameFixRegisterEvent.Item e) {
            super(e);
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.NameFixRegisterEvent.Item.class, e -> NeoForge.EVENT_BUS.post(new Item(e)));
        }
    }

    public static class Biome extends NameFixRegisterEvent {
        public Biome(org.confluence.lib.api.event.NameFixRegisterEvent.Biome e) {
            super(e);
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.NameFixRegisterEvent.Biome.class, e -> NeoForge.EVENT_BUS.post(new Biome(e)));
        }
    }
}
