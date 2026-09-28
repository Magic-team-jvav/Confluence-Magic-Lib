package org.confluence.lib.client.handler;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.LibKeyBindings;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixin.client.LocalPlayerAccessor;
import org.confluence.lib.network.c2s.GravitationPacketC2S;
import org.confluence.lib.network.s2c.BroadcastGravitationRotPacketS2C;

/// 重力反转的客户端处理器（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// 1.20 原文（`Confluence-Magic-Lib` 的 `lib/client/handler/GravitationHandler.java`）**逐字搬运**，
/// 只有 import 换成 Lib 新位置（包路径与 1.20 完全一致，所以实质零改动）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** 三个接线口都还没接：
/// 1. **按键**：`LibKeyBindings.init(...)` 无人调用 → `handle(...)` 里的按键分支不会被触发；
/// 2. **网络**：`GravitationPacketC2S` / `BroadcastGravitationRotPacketS2C` **尚未在
///    `LibModEvents#registerPayloadHandlers` 注册**（按第二步要求不在本步登记）；
/// 3. **调用点**：1.20 是在 `LibClientGameEvents` 里调 `handle`/`force`/`expire`/`unCrouching`/
///    `reset`/`tryExpire`/`isShouldRot`（1.20 `LibClientGameEvents.java:111-148`），
///    1.21 的 `LibClientGameEvents.java:76` 目前只有一行 `// todo 类似1.20.1 GravitationHandler.reset();`
///    —— 第二步把那些调用点补齐。
///
/// 与 1.21 现状的关系：这条特性目前在 **TerraCurio** 的 `terra_curio.client.handler.GravitationHandler`。
/// 本类**只搬 1.20 的形态**（`forceEnable` / `forceCancel` 那套）；1.21 TerraCurio 那份多出来的
/// `hasGlobe` 与 `StepStoolHandler.onStool()` 耦合属 1.21 独有内容，**取舍留给第二步**，本步不擅自加。
public final class GravitationHandler {
    private static final Vec3 DOWN = new Vec3(0.0, -0.3000001, 0.0);
    private static boolean keyDown = false;
    private static boolean shouldRot = false;
    private static boolean forceEnable = false;
    private static boolean forceCancel = false;

    public static void handle(LocalPlayer player) {
        if (isForceCancel()) return;

        if (LibKeyBindings.FLIP_GRAVITATION.get().isDown()) {
            if (!keyDown) {
                shouldRot = !shouldRot;
                player.resetFallDistance();
                GravitationPacketC2S.sendToServer(shouldRot);
            }
            keyDown = true;
        } else {
            keyDown = false;
        }
    }

    public static void force(LocalPlayer player) {
        if (isForceCancel() || player.getAbilities().flying) return;

        if (!shouldRot) {
            shouldRot = true;
            player.resetFallDistance();
            GravitationPacketC2S.sendToServer(true);
        }
    }

    public static void expire() {
        if (shouldRot) {
            shouldRot = false;
            GravitationPacketC2S.sendToServer(false);
        }
    }

    /// LocalPlayer Only
    public static boolean isShouldRot() {
        return shouldRot;
    }

    public static void tryExpire(LocalPlayer player) {
        if (player.getY() > player.level().getMaxBuildHeight()) {
            expire();
        }
    }

    public static void reset() {
        shouldRot = false;
        forceEnable = false;
    }

    public static void unCrouching(Player player) {
        if (shouldRot && player.onGround() && player.isCrouching() && !player.isShiftKeyDown()) {
            player.move(MoverType.SELF, DOWN);
            player.setPose(Pose.STANDING);
            ((LocalPlayerAccessor) player).setCrouching(false);
        }
    }

    public static void setForceEnable(boolean force) {
        forceEnable = force;
    }

    public static boolean isForceEnable() {
        return forceEnable;
    }

    public static void setForceCancel(boolean force) {
        forceCancel = force;
    }

    public static boolean isForceCancel() {
        return forceCancel;
    }

    public static void handleRemoteRot(BroadcastGravitationRotPacketS2C packet, Player player) {
        Entity entity = player.level().getEntity(packet.entityId());
        if (entity != null) {
            ILibEntity.of(entity).confluence$setShouldRot(packet.enabled());
        }
    }

    public static float getJumpDir() {
        return isShouldRot() ? -1.0F : 1.0F;
    }
}
