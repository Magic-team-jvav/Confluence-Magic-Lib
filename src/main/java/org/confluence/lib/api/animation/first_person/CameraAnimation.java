package org.confluence.lib.api.animation.first_person;

import net.neoforged.neoforge.client.event.ViewportEvent;
import software.bernie.geckolib.cache.object.GeoBone;

/// 1.20 侧这里写的是 geckolib 4.4 时代的 `software.bernie.geckolib.core.animatable.model.CoreGeoBone`；
/// 1.21 用的 geckolib 4.8.4 **没有 `core` 包**，骨骼类型是
/// `software.bernie.geckolib.cache.object.GeoBone`（`getRotX/Y/Z` 仍在），
/// 与本仓库既有写法一致（`lib/integration/animation/PlayerAnimationState.java:8`）。
public final class CameraAnimation {
    private static final float EPSILON = 0.0001F;

    private static float pitch;
    private static float yaw;
    private static float roll;

    private CameraAnimation() {}

    /// 捕获camera骨骼
    public static void capture(GeoBone cameraBone) {
        if (cameraBone == null) {
            clear();
            return;
        }
        pitch = (float) Math.toDegrees(cameraBone.getRotX());
        yaw = (float) Math.toDegrees(cameraBone.getRotY());
        roll = (float) Math.toDegrees(cameraBone.getRotZ());
    }

    public static void clear() {
        pitch = 0.0F;
        yaw = 0.0F;
        roll = 0.0F;
    }

    public static void apply(ViewportEvent.ComputeCameraAngles event) {
        if (Math.abs(pitch) < EPSILON && Math.abs(yaw) < EPSILON && Math.abs(roll) < EPSILON) {
            return;
        }
        if (event.getCamera().isDetached()) {
            return;
        }
        event.setPitch(event.getPitch() + pitch);
        event.setYaw(event.getYaw() + yaw);
        event.setRoll(event.getRoll() + roll);
    }
}
