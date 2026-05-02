package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.neoforged.neoforge.common.NeoForge;
import org.confluence.lib.api.event.PlayerNaturalHealEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FoodData.class)
public abstract class FoodDataMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;heal(F)V"))
    private void naturalHeal(Player player, float amount, Operation<Void> original) {
        PlayerNaturalHealEvent event = new PlayerNaturalHealEvent(player, amount);
        if (!NeoForge.EVENT_BUS.post(event).isCanceled()) {
            original.call(player, event.getAmount());
        }
    }
}
