package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.mixed.ILibAbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

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

    @WrapOperation(method = "renderSlotContents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V"))
    private void renderGroupBackground(GuiGraphicsExtractor instance, ItemStack itemStack, int x, int y, int seed, Operation<Void> original, @Local(argsOnly = true, name = "slot") Slot slot) {
        if (confluence$shouldRenderGroupBackground) {
            ILibAbstractContainerScreen.renderGroupBackground(instance, itemStack, x, y, slot);
        }
        original.call(instance, itemStack, x, y, seed);
    }
}
