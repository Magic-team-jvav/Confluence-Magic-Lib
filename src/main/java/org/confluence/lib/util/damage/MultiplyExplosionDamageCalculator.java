package org.confluence.lib.util.damage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.mesdag.portlib.wrapper.world.level.PortExplosionDamageCalculator;

public class MultiplyExplosionDamageCalculator extends PortExplosionDamageCalculator {
    private final float multiplier;

    public MultiplyExplosionDamageCalculator(float multiplier) {
        this.multiplier = multiplier;
    }

    @Override
    public float modifyEntityDamage(Explosion explosion, Entity entity, float originalDamage) {
        return originalDamage * multiplier;
    }
}
