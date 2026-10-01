package org.confluence.lib.mixin.client;

import net.minecraft.world.item.ItemStack;
import org.confluence.lib.mixed.ILibClientItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemStack.class)
public class ClientItemStackMixin implements ILibClientItemStack {
    @Unique
    private int confluence$groupId = -1;

    @Override
    public void confluence$clientSetGroupId(int id) {
        this.confluence$groupId = id;
    }

    @Override
    public int confluence$clientGetGroupId() {
        return confluence$groupId;
    }
}
