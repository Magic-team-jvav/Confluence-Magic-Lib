package org.confluence.lib.mixin;

import net.minecraft.world.damagesource.DamageSource;
import org.confluence.lib.api.projectile.ProjectileCombatSnapshot;
import org.confluence.lib.mixed.ILibDamageSource;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(DamageSource.class)
public class DamageSourceMixin implements ILibDamageSource {
    @Unique
    private boolean confluence$critical;
    @Unique
    private @Nullable ProjectileCombatSnapshot confluence$combatSnapshot;

    @Override
    public void confluence$setCritical(boolean critical) {
        confluence$critical = critical;
    }

    @Override
    public boolean confluence$isCritical() {
        return confluence$critical;
    }

    @Override
    public void confluence$setCombatSnapshot(@Nullable ProjectileCombatSnapshot snapshot) {
        confluence$combatSnapshot = snapshot;
    }

    @Override
    public @Nullable ProjectileCombatSnapshot confluence$getCombatSnapshot() {
        return confluence$combatSnapshot;
    }
}
