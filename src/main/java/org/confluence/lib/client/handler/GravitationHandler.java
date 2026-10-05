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
