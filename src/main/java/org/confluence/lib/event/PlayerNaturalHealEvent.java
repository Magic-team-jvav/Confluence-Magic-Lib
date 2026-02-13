package org.confluence.lib.event;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

public class PlayerNaturalHealEvent extends PlayerEvent implements ICancellableEvent {
    private final float originalAmount;
    private float amount;

    public PlayerNaturalHealEvent(Player player, float amount) {
        super(player);
        this.originalAmount = amount;
        this.amount = amount;
    }

    public float getOriginalAmount() {
        return originalAmount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }
}
