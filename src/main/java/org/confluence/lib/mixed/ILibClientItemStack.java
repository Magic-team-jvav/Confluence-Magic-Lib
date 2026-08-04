package org.confluence.lib.mixed;

import net.minecraft.world.item.ItemStack;

public interface ILibClientItemStack {
    void confluence$setGroupId(int id);

    int confluence$getGroupId();

    static ILibClientItemStack of(ItemStack stack) {
        return (ILibClientItemStack) (Object) stack;
    }
}
