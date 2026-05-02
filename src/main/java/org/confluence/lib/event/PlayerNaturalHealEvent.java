package org.confluence.lib.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.ApiStatus;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public class PlayerNaturalHealEvent extends Event implements ICancellableEvent {
    private final org.confluence.lib.api.event.PlayerNaturalHealEvent e;

    public PlayerNaturalHealEvent(org.confluence.lib.api.event.PlayerNaturalHealEvent e) {
        this.e = e;
    }

    public Player getEntity() {
        return e.getEntity();
    }

    public float getOriginalAmount() {
        return e.getOriginalAmount();
    }

    public void setAmount(float amount) {
        e.setAmount(amount);
    }

    public float getAmount() {
        return e.getOriginalAmount();
    }

    static {
        NeoForge.EVENT_BUS.addListener(org.confluence.lib.api.event.PlayerNaturalHealEvent.class, e -> NeoForge.EVENT_BUS.post(new PlayerNaturalHealEvent(e)));
    }
}
