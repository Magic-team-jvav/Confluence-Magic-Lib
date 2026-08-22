package org.confluence.lib.mixed;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.network.c2s.SwitchEffectEnabledPackedC2S;
import org.confluence.lib.util.LibUtils;
import org.jetbrains.annotations.ApiStatus;
import org.mesdag.portlib.client.gui.components.PortSprite;
import org.mesdag.portlib.wrapper.common.util.PortTriState;

public interface ILibAbstractContainerScreen {
    void confluence$setShouldRenderGroupBackground(boolean should);

    boolean confluence$shouldRenderGroupBackground();

    @ApiStatus.OverrideOnly
    default PortTriState confluence$onMouseClicked(double mouseX, double mouseY, int button) {
        return PortTriState.DEFAULT;
    }

    static ILibAbstractContainerScreen of(AbstractContainerScreen<?> screen) {
        return (ILibAbstractContainerScreen) screen;
    }

    PortSprite TAB_ITEMS = new PortSprite(ConfluenceMagicLib.asResource("creative_inventory_tab_items"), 38, 19);

    static void renderGroupBackground(GuiGraphics instance, ItemStack stack, int x, int y, Slot slot) {
        int id = getGroupId(stack);
        if (id == -1) return;
        int index = slot.getContainerSlot();
        if (index % 9 != 8 && hasNeighbour(index + 10, slot, id)) { // 右下
            int u, v, X, Y, w, h;
            if (hasNeighbour(index - 9, slot, id)) { // 上
                v = 1;
                Y = y;
                h = 18;
            } else {
                v = 0;
                Y = y - 1;
                h = 19;
            }
            if (index % 9 != 0 && hasNeighbour(index - 1, slot, id)) { // 左
                u = 1;
                X = x;
                w = 18;
            } else {
                u = 0;
                X = x - 1;
                w = 19;
            }
            instance.blitSprite(TAB_ITEMS, 38, 19, u, v, X, Y, w, h);
        } else {
            if (index % 9 != 8 && hasNeighbour(index + 1, slot, id)) { // 右
                if (hasNeighbour(index - 9, slot, id)) { // 上
                    instance.blitSprite(TAB_ITEMS, 38, 19, 36, 1, x + 16, y, 2, 17);
                } else {
                    instance.blitSprite(TAB_ITEMS, 38, 19, 36, 0, x + 16, y - 1, 2, 18);
                }
            }
            if (hasNeighbour(index + 9, slot, id)) { // 下
                if (index % 9 != 0 && hasNeighbour(index - 1, slot, id)) { // 左
                    instance.blitSprite(TAB_ITEMS, 38, 19, 20, 17, x, y + 16, 17, 2);
                } else {
                    instance.blitSprite(TAB_ITEMS, 38, 19, 19, 17, x - 1, y + 16, 18, 2);
                }
            }
        }
    }

    private static boolean hasNeighbour(int index, Slot slot, int id) {
        return index >= 0 && index < slot.container.getContainerSize() && getGroupId(slot.container.getItem(index)) == id;
    }

    private static int getGroupId(ItemStack stack) {
        if (stack.is(GroupItem.getInstance())) {
            return stack.getOrDefault(ConfluenceMagicLib.GROUP_STACKS, GroupItem.Stacks.EMPTY).getId();
        }
        return ILibClientItemStack.of(stack).confluence$clientGetGroupId();
    }

    static void switchEnabled(MobEffectInstance instance) {
        if (LibUtils.isSwitchableEffect(instance)) {
            ILibMobEffectInstance i = ILibMobEffectInstance.of(instance);
            i.confluence$setEnabled(!i.confluence$isEnabled());
            SwitchEffectEnabledPackedC2S.sendToServer(instance.getEffect(), i.confluence$isEnabled());
        }
    }

    static void makeTranslucent(GuiGraphics guiGraphics, MobEffectInstance instance, Runnable call) {
        if (ILibMobEffectInstance.of(instance).confluence$isEnabled()) {
            call.run();
        } else {
            RenderSystem.enableBlend();
            guiGraphics.setColor(1, 1, 1, 0.5F);
            call.run();
            guiGraphics.setColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
        }
    }
}
