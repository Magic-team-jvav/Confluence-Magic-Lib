package org.confluence.lib.api.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.LivingEvent;

public class ArmorPenetrationEvent extends LivingEvent {
    private final DamageSource damageSource;
    private final float armorValue;
    private float penetration;

    public ArmorPenetrationEvent(LivingEntity victim, DamageSource damageSource, float armorValue) {
        super(victim);
        this.damageSource = damageSource;
        this.armorValue = armorValue;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getArmorValue() {
        return armorValue;
    }

    public void setPenetration(float penetration) {
        this.penetration = penetration;
    }

    public float getPenetration() {
        return penetration;
    }
}
