package org.confluence.lib.mixed;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.GroupItem;

public interface ILibAbstractContainerScreen {
    void confluence$setShouldRenderGroupBackground(boolean should);

    boolean confluence$shouldRenderGroupBackground();

    static ILibAbstractContainerScreen of(AbstractContainerScreen<?> screen) {
        return (ILibAbstractContainerScreen) screen;
    }

    ResourceLocation TAB_ITEMS = ConfluenceMagicLib.asResource("creative_inventory_tab_items");

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
        return IClientItemStack.of(stack).confluence$getGroupId();
    }
}
