package org.confluence.lib.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public abstract class SwitchItemFunctionEvent extends Event {
    protected final org.confluence.lib.api.event.SwitchItemFunctionEvent e;

    public SwitchItemFunctionEvent(org.confluence.lib.api.event.SwitchItemFunctionEvent e) {
        this.e = e;
    }

    public Player getEntity() {
        return e.getEntity();
    }

    public ItemStack getStack() {
        return e.getStack();
    }

    public static class Pre extends SwitchItemFunctionEvent implements ICancellableEvent {
        public Pre(org.confluence.lib.api.event.SwitchItemFunctionEvent.Pre e) {
            super(e);
        }

        @Override
        public void setCanceled(boolean canceled) {
            ICancellableEvent.super.setCanceled(canceled);
            ((org.confluence.lib.api.event.SwitchItemFunctionEvent.Pre) e).setCanceled(canceled);
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.SwitchItemFunctionEvent.Pre.class, e -> NeoForge.EVENT_BUS.post(new SwitchItemFunctionEvent.Pre(e)));
        }
    }

    public static class Post extends SwitchItemFunctionEvent {
        public Post(org.confluence.lib.api.event.SwitchItemFunctionEvent.Post e) {
            super(e);
        }

        public boolean isEnabled() {
            return ((org.confluence.lib.api.event.SwitchItemFunctionEvent.Post) e).isEnabled();
        }

        static {
            NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.SwitchItemFunctionEvent.Post.class, e -> NeoForge.EVENT_BUS.post(new SwitchItemFunctionEvent.Post(e)));
        }
    }
}
