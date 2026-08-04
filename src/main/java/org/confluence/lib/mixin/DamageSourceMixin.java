package org.confluence.lib.mixin;

import net.minecraft.world.damagesource.DamageSource;
import org.confluence.lib.mixed.ILibDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DamageSource.class)
public class DamageSourceMixin implements ILibDamageSource {
    @Unique
    private boolean confluence$critical;

    @Override
    public void confluence$setCritical(boolean critical) {
        confluence$critical = critical;
    }

    @Override
    public boolean confluence$isCritical() {
        return confluence$critical;
    }
}
