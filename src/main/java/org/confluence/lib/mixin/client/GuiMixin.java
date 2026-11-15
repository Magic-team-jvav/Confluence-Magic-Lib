package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import org.confluence.lib.api.event.RenderSelectedItemNameEvent;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @ModifyExpressionValue(method = "renderEffects", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/effect/MobEffectInstance;showIcon()Z"))
    private boolean skip(boolean original, @Local MobEffectInstance instance) {
        if (original && !ILibMobEffectInstance.of(instance).confluence$isEnabled()) {
            return false;
        }
        return original;
    }

    @WrapOperation(method = "renderSelectedItemName(Lnet/minecraft/client/gui/GuiGraphics;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getHighlightTip(Lnet/minecraft/network/chat/Component;)Lnet/minecraft/network/chat/Component;"))
    private Component modRarity(ItemStack instance, Component component, Operation<Component> original) {
        Component call = original.call(instance, component);
        int color = ModRarity.chooseColor(instance);
        Component withRarityColor = color == -1 ? call : call.copy().withColor(color);
        return NeoForge.EVENT_BUS.post(new RenderSelectedItemNameEvent(instance, withRarityColor)).getOverride();
    }
}
