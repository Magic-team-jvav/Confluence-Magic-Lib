package org.confluence.lib.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;

/**
 * 忽略对使用者和玩家伤害
 */
public class IgnoreThrowerExplosionDamageCalculator extends MultiplyExplosionDamageCalculator {
    private final Entity thrower;

    public IgnoreThrowerExplosionDamageCalculator(float multiplier, Entity thrower) {
        super(multiplier);
        this.thrower = thrower;
    }

    @Override
    public float getEntityDamageAmount(Explosion explosion, Entity entity) {
        if (entity == thrower || entity instanceof Player) {
            return 0.0F;
        }
        return super.getEntityDamageAmount(explosion, entity);
    }
}
