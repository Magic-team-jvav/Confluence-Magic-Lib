package org.confluence.lib.util;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public class IgnoreThrowerExplosionDamageCalculator extends org.confluence.lib.util.damage.IgnoreThrowerExplosionDamageCalculator {
    public IgnoreThrowerExplosionDamageCalculator(float multiplier, LivingEntity thrower) {
        super(multiplier, thrower);
    }
}
