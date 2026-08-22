package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.confluence.lib.util.LibUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class MobEffectInstanceMixin implements ILibMobEffectInstance {
    @Unique
    private boolean confluence$enabled = true;

    @Override
    public void confluence$setEnabled(boolean enabled) {
        this.confluence$enabled = enabled;
    }

    @Override
    public boolean confluence$isEnabled() {
        return confluence$enabled;
    }

    @ModifyReturnValue(method = "save", at = @At("RETURN"))
    private CompoundTag saveExtra(CompoundTag original) {
        original.putBoolean("confluence:is_enabled", confluence$isEnabled());
        return original;
    }

    @Inject(method = "load", at = @At("RETURN"))
    private static void loadExtra(CompoundTag nbt, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (nbt.contains("confluence:is_enabled")) {
            ILibMobEffectInstance.of(cir.getReturnValue()).confluence$setEnabled(nbt.getBoolean("confluence:is_enabled"));
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffect;isDurationEffectTick(II)Z"))
    private boolean skip(boolean original) {
        if (!confluence$enabled) {
            return false;
        }
        return original;
    }

    @Inject(method = "update", at = @At("HEAD"))
    private void merge(MobEffectInstance other, CallbackInfoReturnable<Boolean> cir) {
        if (!ILibMobEffectInstance.of(other).confluence$isEnabled()) {
            confluence$setEnabled(false);
        } else if (!confluence$isEnabled() && !LibUtils.isSwitchableEffect(other)) {
            confluence$setEnabled(true);
        }
    }
}
