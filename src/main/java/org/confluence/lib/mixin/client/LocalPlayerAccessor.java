package org.confluence.lib.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/// `LocalPlayer#crouching` 字段的 setter 访问器（`GravitationHandler#unCrouching` 要用：
/// 重力反转时玩家在「地面」上但姿态卡在蹲伏，需要强制解除）。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/client/LocalPlayerAccessor.java`）逐字。
///
/// 1.21 侧此前**没有**这个 accessor（Lib 的 `mixin/client/` 下只有 `LocalPlayerMixin`），所以按 1.20 补。
/// 目标字段核实（1.21 源码）：`LocalPlayer` 有 `private boolean crouching;`（非 final）——
/// Mixin 的 `@Accessor` 按命名约定由 `setCrouching` 推出字段名 `crouching`，与 1.20 同一套写法。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** 唯一使用者 `GravitationHandler#unCrouching`
/// 目前没有调用点（1.20 是在 `LibClientGameEvents` 里调）。
@Mixin(LocalPlayer.class)
public interface LocalPlayerAccessor {
    @Accessor
    void setCrouching(boolean crouching);
}
