package org.confluence.lib.mixin.client;

import net.minecraft.world.entity.Entity;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// 重力反转时把「眼睛高度」压到实体高度的一小部分（客户端表现）。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/client/ClientEntityMixin.java`）逐字。
///
/// 注入点核实（1.21 源码）：`Entity#getEyeHeight()` 存在（`public final float getEyeHeight()`）——
/// 目标是方法本体（无参重载），对 final 方法做 `@Inject` 是允许的（不是覆写）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** `confluence$isShouldRot()` 恒为 `false` → no-op。
@Mixin(Entity.class)
public abstract class ClientEntityMixin implements SelfGetter<Entity> {
    @Inject(method = "getEyeHeight()F", at = @At("RETURN"), cancellable = true)
    private void eyeHeight(CallbackInfoReturnable<Float> cir) {
        ILibEntity self = ILibEntity.of(confluence$self());
        if (self.confluence$isShouldRot()) {
            cir.setReturnValue(self.confluence$getDimensionHeight() * 0.15F);
        }
    }
}
