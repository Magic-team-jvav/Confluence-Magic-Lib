package org.confluence.lib.mixed;

import net.minecraft.world.item.ItemStack;

/// 仅客户端可用的 ItemStack 扩展 API；调用方必须确保只在物理客户端执行。
public interface ILibClientItemStack {
    void confluence$clientSetGroupId(int id);

    int confluence$clientGetGroupId();

    static ILibClientItemStack of(ItemStack stack) {
        return (ILibClientItemStack) (Object) stack;
    }
}
