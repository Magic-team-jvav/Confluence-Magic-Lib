package org.confluence.lib.util;

import org.jetbrains.annotations.ApiStatus;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public class MultiplyExplosionDamageCalculator extends org.confluence.lib.util.damage.MultiplyExplosionDamageCalculator {
    public MultiplyExplosionDamageCalculator(float multiplier) {
        super(multiplier);
    }
}
