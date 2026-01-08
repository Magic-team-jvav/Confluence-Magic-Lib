package org.confluence.lib.common.item;

import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.common.component.NbtComponent;
import org.confluence.lib.util.LibUtils;
import org.jetbrains.annotations.ApiStatus;

public class ColoredItem extends CustomRarityItem {
    public ColoredItem(Properties properties, ModRarity rarity) {
        super(properties, rarity);
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (ItemStack.isSameItem(stack, other)) {
            setRGBA(other, getRGBA(stack));
        }
        return false;
    }

    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static void setColor(ItemStack itemStack, int rgb) {
        setRGBA(itemStack, rgb);
    }

    public static void setRGBA(ItemStack itemStack, int rgba) {
        LibUtils.updateItemStackNbt(itemStack, tag -> tag.putInt("color", rgba));
    }

    @Deprecated(since = "1.2.0", forRemoval = true)
    @ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
    public static int getColor(ItemStack itemStack) {
        return getRGBA(itemStack);
    }

    public static int getRGBA(ItemStack itemStack) {
        NbtComponent nbtComponent = itemStack.get(ConfluenceMagicLib.NBT);
        if (nbtComponent == null) {
            return 0xFF66CCFF;
        }
        return nbtComponent.nbt().getInt("color");
    }

    public static void merge(ItemStack carried, ItemStack onSlot) {
        if (onSlot.getItem() instanceof ColoredItem && ItemStack.isSameItem(onSlot, carried)) {
            setRGBA(carried, getRGBA(onSlot));
        }
    }
}
