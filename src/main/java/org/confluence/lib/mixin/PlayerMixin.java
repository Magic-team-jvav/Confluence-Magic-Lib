package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.mixed.ILibDamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    private ItemStack lastItemInMainHand;

    @Inject(method = "resetAttackStrengthTicker", at = @At("HEAD"), cancellable = true)
    private void denyReset(CallbackInfo ci) {
        if (lastItemInMainHand.is(LibTags.Items.SKIP_RESET_STRENGTH)) {
            ci.cancel();
        }
    }

    @WrapOperation(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean attack(Entity instance, DamageSource source, float amount, Operation<Boolean> original, @Local(ordinal = 2) boolean flag2) {
        ILibDamageSource lds = ILibDamageSource.of(source);
        if (lds != null) lds.confluence$setCritical(flag2);
        return original.call(instance, source, amount);
    }
}
