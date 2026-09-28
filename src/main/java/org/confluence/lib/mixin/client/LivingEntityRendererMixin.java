package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.mixed.ILibEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// 重力反转时把生物渲染成「倒过来」（客户端表现）。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/client/LivingEntityRendererMixin.java`）逐字。
///
/// 注入点核实（1.21 源码，`LivingEntityRenderer#isEntityUpsideDown(LivingEntity)` 是 `public static`）：
/// 方法体里正好两个 `return`（`:275` 的 Dinnerbone/Grumm 判断、`:279` 的兜底 `return false`），
/// `ordinal = 1` 命中后者 —— 与 1.20 一致。处理器也是 `private static`（静态目标必须配静态处理器）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** `confluence$isShouldRot()` 恒为 `false` → no-op。
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @ModifyReturnValue(method = "isEntityUpsideDown", at = @At(value = "RETURN", ordinal = 1))
    private static boolean upsideDown(boolean original, @Local(argsOnly = true) LivingEntity living) {
        if (!original && ILibEntity.of(living).confluence$isShouldRot()) {
            return true;
        }
        return original;
    }
}
