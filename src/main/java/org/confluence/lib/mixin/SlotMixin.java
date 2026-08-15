package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.util.LibUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 放宽普通原版槽位的 64 堆叠上限。
 *
 * <p>物品本身已经能声明更大的堆叠数量，但玩家背包、箱子和普通菜单槽位仍会通过
 * {@link Slot#getMaxStackSize()} 把合并上限限制回 64。这里让普通槽位尊重物品自己的上限，
 * 使箭矢这类被配置为 9999 的物品放入背包后不会被拆回 64。
 *
 * <p>如果某个特殊槽位原本明确把上限压到 64 以下，说明它有单独规则，例如只能放 1 个物品。
 * 这种槽位不会被放宽，避免把修复普通背包堆叠的问题扩散成新的兼容问题。
 */
@Mixin(Slot.class)
public abstract class SlotMixin {
    @ModifyReturnValue(method = "getMaxStackSize()I", at = @At("RETURN"))
    private int confluence$expandGenericSlotLimit(int original) {
        if (original < 64) {
            return original;
        }
        return LibUtils.getMaxStackSize(original);
    }

    @ModifyReturnValue(method = "getMaxStackSize(Lnet/minecraft/world/item/ItemStack;)I", at = @At("RETURN"))
    private int confluence$respectExpandedItemLimit(int original, ItemStack stack) {
        if (stack.isEmpty() || original < 64) {
            return original;
        }
        return Math.max(original, stack.getMaxStackSize());
    }
}
