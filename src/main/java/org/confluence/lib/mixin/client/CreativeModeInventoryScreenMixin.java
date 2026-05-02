package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.mixed.ILibAbstractContainerScreen;
import org.confluence.lib.mixed.ILibCreativeModeTab;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin implements SelfGetter<CreativeModeInventoryScreen> {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Unique
    private CreativeModeTab confluence$lastCreativeModeTab;

    @Shadow
    protected abstract void refreshCurrentTabContents(Collection<ItemStack> items);

    @Unique
    private void confluence$check() {
        if (confluence$lastCreativeModeTab != selectedTab) {
            this.confluence$lastCreativeModeTab = selectedTab;
            ILibAbstractContainerScreen.of(confluence$self()).confluence$setShouldRenderGroupBackground(
                    LibStartupConfig.itemGroups() && !GroupItem.isInvalidCreativeModeTab(selectedTab)
            );
        }
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        confluence$check();
    }

    @Inject(method = "containerTick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        confluence$check();
    }

    @Inject(method = "slotClicked", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screens/inventory/CreativeModeInventoryScreen$ItemPickerMenu;getCarried()Lnet/minecraft/world/item/ItemStack;", ordinal = 7), cancellable = true)
    private void checkIsGroupItem(CallbackInfo ci, @Local(argsOnly = true) Slot slot) {
        if (ILibAbstractContainerScreen.of(confluence$self()).confluence$shouldRenderGroupBackground()) {
            ItemStack stack = slot.getItem();
            if (stack.is(GroupItem.getInstance())) {
                GroupItem.toggleVisibility(stack);
                ILibCreativeModeTab.of(selectedTab).confluence$buildGroup();
                refreshCurrentTabContents(selectedTab.getDisplayItems());
                ci.cancel();
            }
        }
    }
}
