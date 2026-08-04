package org.confluence.lib.api.event;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingEvent;

public class ProcessCriticalDamageEvent extends LivingEvent {
    private final DamageSource damageSource;
    private final boolean originalCritical;
    private final float originalAmount;
    private float amount;
    private boolean critical;
    private float criticalDamageMultiplier = 1.5F;

    public ProcessCriticalDamageEvent(LivingEntity victim, DamageSource damageSource, float originalAmount, boolean originalCritical) {
        super(victim);
        this.damageSource = damageSource;
        this.originalCritical = originalCritical;
        this.originalAmount = originalAmount;
        this.amount = originalAmount;
        this.critical = originalCritical;
    }

    public DamageSource getDamageSource() {
        return damageSource;
    }

    public float getCriticalDamageMultiplier() {
        return criticalDamageMultiplier;
    }

    public void setCriticalDamageMultiplier(float multiplier) {
        this.criticalDamageMultiplier = multiplier;
    }

    public float getOriginalAmount() {
        return originalAmount;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public boolean isOriginalCritical() {
        return originalCritical;
    }

    public boolean isCritical() {
        return critical;
    }

    public void setCritical(boolean critical) {
        this.critical = critical;
    }
}
