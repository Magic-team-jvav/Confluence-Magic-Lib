package org.confluence.lib.api.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class PlayerNaturalHealEvent extends PlayerEvent {
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
