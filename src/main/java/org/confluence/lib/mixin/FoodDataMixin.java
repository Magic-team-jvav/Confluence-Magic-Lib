package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.common.NeoForge;
import org.confluence.lib.api.event.PlayerNaturalHealEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V"))
    private void naturalHeal(ServerPlayer instance, float heal, Operation<Void> original) {
        PlayerNaturalHealEvent event = new PlayerNaturalHealEvent(instance, heal);
        if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
            original.call(instance, event.getAmount());
        }
    }
}
