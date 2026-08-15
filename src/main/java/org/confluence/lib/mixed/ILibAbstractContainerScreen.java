package org.confluence.lib.mixed;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.GroupItem;
import org.mesdag.portlib.wrapper.PortUtil;

public interface ILibAbstractContainerScreen {
    int GRID_COLUMNS = 9;
    int SLOT_SIZE = 18;
    int SPRITE_WIDTH = 38;
    int SPRITE_HEIGHT = 19;

    void confluence$setShouldRenderGroupBackground(boolean should);

    boolean confluence$shouldRenderGroupBackground();

    static ILibAbstractContainerScreen of(AbstractContainerScreen<?> screen) {
        return (ILibAbstractContainerScreen) screen;
    }

    ResourceLocation TAB_ITEMS = PortUtil.asGuiSprite(ConfluenceMagicLib.LIB_ID, "creative_inventory_tab_items");

    static void renderGroupBackground(GuiGraphics graphics, ItemStack stack, int x, int y, Slot slot) {
        int groupId = getGroupId(stack);
        if (groupId == -1) {
            return;
        }

        int index = slot.getContainerSlot();
        boolean hasLeft = index % GRID_COLUMNS != 0 && hasNeighbour(index - 1, slot, groupId);
        boolean hasRight = index % GRID_COLUMNS != GRID_COLUMNS - 1 && hasNeighbour(index + 1, slot, groupId);
        boolean hasUp = hasNeighbour(index - GRID_COLUMNS, slot, groupId);
        boolean hasDown = hasNeighbour(index + GRID_COLUMNS, slot, groupId);
        boolean hasRightDown = index % GRID_COLUMNS != GRID_COLUMNS - 1
                && hasNeighbour(index + GRID_COLUMNS + 1, slot, groupId);

        /*
         * 分组背景使用一张 38x19 的小贴图拼接。每个格子只绘制自己负责的左上主体区域，
         * 最右列和最下行再补边，这样相邻物品共享同一条边，不会出现双线或一像素缝隙。
         */
        if (hasRightDown) {
            int u = hasLeft ? 1 : 0;
            int v = hasUp ? 1 : 0;
            int renderX = hasLeft ? x : x - 1;
            int renderY = hasUp ? y : y - 1;
            int width = hasLeft ? SLOT_SIZE : SLOT_SIZE + 1;
            int height = hasUp ? SLOT_SIZE : SLOT_SIZE + 1;
            graphics.blit(TAB_ITEMS, renderX, renderY, u, v, width, height, SPRITE_WIDTH, SPRITE_HEIGHT);
            return;
        }

        // 右侧还有同组物品时，当前格子负责补右边界，避免下一格左边界重复绘制。
        if (hasRight) {
            if (hasUp) {
                graphics.blit(TAB_ITEMS, x + SLOT_SIZE - 2, y, 36, 1, 2, SLOT_SIZE - 1, SPRITE_WIDTH, SPRITE_HEIGHT);
            } else {
                graphics.blit(TAB_ITEMS, x + SLOT_SIZE - 2, y - 1, 36, 0, 2, SLOT_SIZE, SPRITE_WIDTH, SPRITE_HEIGHT);
            }
        }

        // 下方还有同组物品时，当前格子负责补下边界，避免下一行上边界重复绘制。
        if (hasDown) {
            if (hasLeft) {
                graphics.blit(TAB_ITEMS, x, y + SLOT_SIZE - 2, 20, 17, SLOT_SIZE - 1, 2, SPRITE_WIDTH, SPRITE_HEIGHT);
            } else {
                graphics.blit(TAB_ITEMS, x - 1, y + SLOT_SIZE - 2, 19, 17, SLOT_SIZE, 2, SPRITE_WIDTH, SPRITE_HEIGHT);
            }
        }
    }

    private static boolean hasNeighbour(int index, Slot slot, int id) {
        return index >= 0
                && index < slot.container.getContainerSize()
                && getGroupId(slot.container.getItem(index)) == id;
    }

    private static int getGroupId(ItemStack stack) {
        if (stack.is(GroupItem.getInstance())) {
            return stack.getOrDefault(ConfluenceMagicLib.GROUP_STACKS, GroupItem.Stacks.EMPTY).getId();
        }
        return ILibClientItemStack.of(stack).confluence$clientGetGroupId();
    }
}
