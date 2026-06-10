package org.confluence.lib.util;

import PortLib.extensions.com.mojang.blaze3d.vertex.VertexConsumer.PortVertexConsumerExtension;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;

import static org.confluence.lib.client.render.visual_effects.VisualEffects.getCamera;

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

    public static void drawCube(PoseStack poseStack, float ballSide, int red, int green, int blue, int alpha, Vector3f entityPos, Vector3f pos0, boolean face, float rotate0, float rotate1, VertexConsumer consumer) {
        PoseStack.Pose pose = poseStack.last();

        float ballSide1 = ballSide * Mth.SQRT_OF_TWO;
        float cosZ = Mth.cos(rotate1);
        float sinZ = Mth.sin(rotate1);
        float cos0 = Mth.cos(rotate0);
        float sin0 = Mth.sin(rotate0);

        float bs_cos0_div2 = ballSide1 * cos0 * 0.5F;
        float bs_sin0_div2 = ballSide1 * sin0 * 0.5F;
        float bs_div2 = ballSide * 0.5F;

        float x1 = -bs_sin0_div2;
        float x2 = -bs_cos0_div2;
        float z2 = -bs_sin0_div2;
        float x3 = -x1;
        float z3 = -bs_cos0_div2;

        float p0x = bs_cos0_div2 * cosZ - bs_div2 * sinZ;
        float p0y = bs_cos0_div2 * sinZ + bs_div2 * cosZ;

        float p1x = x1 * cosZ - bs_div2 * sinZ;
        float p1y = x1 * sinZ + bs_div2 * cosZ;

        float p2x = x2 * cosZ - bs_div2 * sinZ;
        float p2y = x2 * sinZ + bs_div2 * cosZ;

        float p3x = x3 * cosZ - bs_div2 * sinZ;
        float p3y = x3 * sinZ + bs_div2 * cosZ;

        Vec3 rawCameraPos = getCamera().getPosition();
        float cx = (float) rawCameraPos.x - entityPos.x;
        float cy = (float) rawCameraPos.y - entityPos.y;
        float cz = (float) rawCameraPos.z - entityPos.z;

        float v0x = cx - p0x;
        float v0y = cy - p0y;
        float v0z = cz - bs_sin0_div2;

        float v0Len = Mth.sqrt(v0x * v0x + v0y * v0y + v0z * v0z);
        if (v0Len > 1e-6) {
            v0x /= v0Len;
            v0y /= v0Len;
            v0z /= v0Len;
        }

        float px0 = pos0.x;
        float py0 = pos0.y;
        float pz0 = pos0.z;

        if (face || calculateNormalRaw(p0x, p0y, bs_sin0_div2, -p2x, -p2y, -z2, -p3x, -p3y, -z3, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, p0x + px0, p0y + py0, bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p2x + px0, -p2y + py0, -z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p3x + px0, -p3y + py0, -z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p1x + px0, p1y + py0, bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }

        if (face || calculateNormalRaw(p1x, p1y, bs_cos0_div2, -p3x, -p3y, -z3, -p0x, -p0y, -bs_sin0_div2, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, p1x + px0, p1y + py0, bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p3x + px0, -p3y + py0, -z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p0x + px0, -p0y + py0, -bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p2x + px0, p2y + py0, z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }

        if (face || calculateNormalRaw(p2x, p2y, z2, -p0x, -p0y, -bs_sin0_div2, -p1x, -p1y, -bs_cos0_div2, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, p2x + px0, p2y + py0, z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p0x + px0, -p0y + py0, -bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p1x + px0, -p1y + py0, -bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p3x + px0, p3y + py0, z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }

        if (face || calculateNormalRaw(p3x, p3y, z3, -p1x, -p1y, -bs_cos0_div2, -p2x, -p2y, -z2, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, p3x + px0, p3y + py0, z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p1x + px0, -p1y + py0, -bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p2x + px0, -p2y + py0, -z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p0x + px0, p0y + py0, bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }

        if (face || calculateNormalRaw(p0x, p0y, bs_sin0_div2, p1x, p1y, bs_cos0_div2, p2x, p2y, z2, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, p0x + px0, p0y + py0, bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p1x + px0, p1y + py0, bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p2x + px0, p2y + py0, z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, p3x + px0, p3y + py0, z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }

        if (face || calculateNormalRaw(-p0x, -p0y, -bs_sin0_div2, -p3x, -p3y, -z3, -p2x, -p2y, -z2, v0x, v0y, v0z)) {
            PortVertexConsumerExtension.vertex(consumer, pose, -p0x + px0, -p0y + py0, -bs_sin0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p3x + px0, -p3y + py0, -z3 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p2x + px0, -p2y + py0, -z2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
            PortVertexConsumerExtension.vertex(consumer, pose, -p1x + px0, -p1y + py0, -bs_cos0_div2 + pz0, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
        }
    }

    public static boolean calculateNormalRaw(float p0x, float p0y, float p0z,
                                             float p1x, float p1y, float p1z,
                                             float p2x, float p2y, float p2z,
                                             float cx, float cy, float cz) {

        float dx1 = p1x - p0x;
        float dy1 = p1y - p0y;
        float dz1 = p1z - p0z;

        float dx2 = p2x - p0x;
        float dy2 = p2y - p0y;
        float dz2 = p2z - p0z;

        float nx = dy1 * dz2 - dz1 * dy2;
        float ny = dz1 * dx2 - dx1 * dz2;
        float nz = dx1 * dy2 - dy1 * dx2;

        float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) return false;

        nx /= len;
        ny /= len;
        nz /= len;

        float cLen = Mth.sqrt(cx * cx + cy * cy + cz * cz);
        if (cLen < 1e-6) return false;
        cx /= cLen;
        cy /= cLen;
        cz /= cLen;

        float dotProduct = nx * cx + ny * cy + nz * cz;

        return dotProduct > 0;
    }
}
