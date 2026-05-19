package org.confluence.lib.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.player.Player;
import org.joml.Quaternionf;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

public final class LibRenderUtils {
    public static final Quaternionf ANGLE_45 = Axis.YP.rotationDegrees(45);
    public static final Quaternionf ANGLE_180 = Axis.ZP.rotationDegrees(180);
    public static final Quaternionf ANGLE_N90 = Axis.YP.rotationDegrees(-90);
    public static final int[] FULL_BRIGHT = {0xF000F0, 0xF000F0, 0xF000F0, 0xF000F0};

    public static void setupOverlayRenderState(boolean blend, boolean depthTest) {
        if (blend) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.disableBlend();
        }

        if (depthTest) {
            RenderSystem.enableDepthTest();
        } else {
            RenderSystem.disableDepthTest();
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
    }

    public static boolean shouldDrawSurvivalElements(Minecraft minecraft) {
        return minecraft.gameMode.canHurtPlayer() && minecraft.getCameraEntity() instanceof Player;
    }

    public static boolean isBlendEnabled() {
        return GL11.glIsEnabled(GL11.GL_BLEND);
    }

    public static boolean isDepthTestEnabled() {
        return GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
    }

    /// @return {srcRGB, dstRGB, srcAlpha, dstAlpha}
    public static int[] getBlendFunc() {
        return new int[]{
                GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),
                GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),
                GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
        };
    }

    public static boolean getCurrentDepthMask() {
        return GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
    }

    public static boolean isPolygonOffsetEnabled() {
        return GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
    }

    /// @return {factor, units}
    public static float[] getPolygonOffset() {
        return new float[]{
                GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR),
                GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS)
        };
    }

    public static boolean isCullEnabled() {
        return GL11.glIsEnabled(GL11.GL_CULL_FACE);
    }
}
