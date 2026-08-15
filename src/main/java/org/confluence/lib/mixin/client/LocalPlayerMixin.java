package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.LocalPlayer;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin implements SelfGetter<LocalPlayer> {
    /**
     * 仅跳过使用物品时对移动输入施加的减速。
     * <p>
     * 此处必须修改第一处 {@code isUsingItem()} 判断，不能通过伪造
     * {@code isPassenger()} 的返回值绕过分支。后者会让没有载具的玩家被临时视为乘客，
     * 原版在同一次 {@code aiStep()} 中读取载具落地状态时便会访问空载具。
     *
     * @param original 原版的使用物品状态
     * @return 标记物品返回 {@code false} 以跳过减速，其他情况保持原值
     */
    @ModifyExpressionValue(
            method = "aiStep",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z",
                    ordinal = 0
            )
    )
    private boolean skipSlowdown(boolean original) {
        if (original && confluence$self().getUseItem().is(LibTags.Items.SKIP_USING_SLOWDOWN)) {
            return false;
        }
        return original;
    }
}
