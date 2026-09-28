package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.confluence.lib.mixed.ILibEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/// 重力反转时，服务端不要因为「玩家往上飘」而清掉 `fallDistance`。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/ServerGamePacketListenerImplMixin.java`）逐字。
///
/// 注入点核实（1.21 源码）：`ServerGamePacketListenerImpl#handleMovePlayer(ServerboundMovePlayerPacket)`
/// 里 `if (flag4) { this.player.resetFallDistance(); }` —— `this.player` 的静态类型是 `ServerPlayer`，
/// 所以 INVOKE 的 owner 就是 `ServerPlayer`（与 1.20 的 target 一致；同文件另有一处
/// `livingentity.resetFallDistance()` 属 `LivingEntity`，不会被命中）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** `confluence$isShouldRot()` 恒为 `false`
/// → 本条件恒返回 `true`（即「照旧重置」），行为零变化。
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
    @WrapWithCondition(method = "handleMovePlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetFallDistance()V"))
    private boolean doNotReset(ServerPlayer instance) {
        return !ILibEntity.of(instance).confluence$isShouldRot();
    }
}
