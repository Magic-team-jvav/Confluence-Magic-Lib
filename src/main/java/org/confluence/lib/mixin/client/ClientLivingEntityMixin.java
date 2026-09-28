package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 重力反转时，客户端侧把「向上掉落」也累计进 `fallDistance`（与服务端的
/// `EntityMixin#updateFallDistance` 对称）。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/client/ClientLivingEntityMixin.java`）逐字。
///
/// 注入点核实（1.21 源码）：`LivingEntity#checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos)`
/// 存在；`@Local(argsOnly = true) double motionY` 取第 1 个实参（类型匹配，与形参名无关）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** `confluence$isShouldRot()` 恒为 `false` → no-op。
@Mixin(LivingEntity.class)
public abstract class ClientLivingEntityMixin implements SelfGetter<LivingEntity> {
    @Inject(method = "checkFallDamage", at = @At("HEAD"))
    private void fall(CallbackInfo ci, @Local(argsOnly = true) double motionY) {
        if (motionY > 0.0 && ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            confluence$self().fallDistance += (float) motionY;
        }
    }
}
