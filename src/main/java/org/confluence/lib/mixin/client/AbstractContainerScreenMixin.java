package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.mixed.ILibAbstractContainerScreen;
import org.mesdag.portlib.wrapper.common.util.PortTriState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin implements ILibAbstractContainerScreen {
    @Unique
    private boolean confluence$shouldRenderGroupBackground;

    @Override
    public void confluence$setShouldRenderGroupBackground(boolean should) {
        this.confluence$shouldRenderGroupBackground = should;
    }

    @Override
    public boolean confluence$shouldRenderGroupBackground() {
        return confluence$shouldRenderGroupBackground;
    }

    @WrapOperation(method = "renderSlot", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;renderItem(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void renderGroupBackground(GuiGraphics instance, ItemStack stack, int x, int y, int seed, Operation<Void> original, @Local(argsOnly = true) Slot slot) {
        if (confluence$shouldRenderGroupBackground) {
            ILibAbstractContainerScreen.renderGroupBackground(instance, stack, x, y, slot);
        }
        original.call(instance, stack, x, y, seed);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        PortTriState PortTriState = confluence$onMouseClicked(mouseX, mouseY, button);
        if (!PortTriState.isDefault()) cir.setReturnValue(PortTriState.isTrue());
    }
}
