package org.confluence.lib.util.damage;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;

/// 忽略对使用者和使用者无法攻击的对象
public class IgnoreThrowerExplosionDamageCalculator extends MultiplyExplosionDamageCalculator {
    private final LivingEntity thrower;

    public IgnoreThrowerExplosionDamageCalculator(float multiplier, LivingEntity thrower) {
        super(multiplier);
        this.thrower = thrower;
    }

    @Override
    public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
        return !(entity == thrower || (entity instanceof LivingEntity living && !thrower.canAttack(living)));
    }
}
