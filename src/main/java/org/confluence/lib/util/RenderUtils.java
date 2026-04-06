package org.confluence.lib.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import static org.confluence.lib.client.render.visual_effects.VisualEffects.getCamera;

public class RenderUtils {
    public static Vector3d toVector3d(Vec3 vec3) {
        return new Vector3d(vec3.x, vec3.y, vec3.z);
    }

    public static void drawCube(PoseStack poseStack, double ballSide, int red, int green, int blue, int alpha, Vector3d entityPos, Vector3d pos0, boolean face, double rotate0, double rotate1, VertexConsumer consumer) {

        PoseStack.Pose pose = poseStack.last();

        double ballSide1 = ballSide * Math.sqrt(2);
        double cosZ = Math.cos(rotate1);
        double sinZ = Math.sin(rotate1);
        double cos0 = Math.cos(rotate0);
        double sin0 = Math.sin(rotate0);

        double bs_cos0_div2 = ballSide1 * cos0 / 2.0;
        double bs_sin0_div2 = ballSide1 * sin0 / 2.0;
        double bs_div2 = ballSide / 2.0;

        double x1 = -bs_sin0_div2;
        double x2 = -bs_cos0_div2;
        double z2 = -bs_sin0_div2;
        double x3 = -x1;
        double z3 = -bs_cos0_div2;

        double p0x = bs_cos0_div2 * cosZ - bs_div2 * sinZ;
        double p0y = bs_cos0_div2 * sinZ + bs_div2 * cosZ;

        double p1x = x1 * cosZ - bs_div2 * sinZ;
        double p1y = x1 * sinZ + bs_div2 * cosZ;

        double p2x = x2 * cosZ - bs_div2 * sinZ;
        double p2y = x2 * sinZ + bs_div2 * cosZ;

        double p3x = x3 * cosZ - bs_div2 * sinZ;
        double p3y = x3 * sinZ + bs_div2 * cosZ;

        Vector3d rawCameraPos = toVector3d(getCamera().getPosition());
        double cx = rawCameraPos.x - entityPos.x;
        double cy = rawCameraPos.y - entityPos.y;
        double cz = rawCameraPos.z - entityPos.z;

        double v0x = cx - p0x;
        double v0y = cy - p0y;
        double v0z = cz - bs_sin0_div2;

        double v0Len = Math.sqrt(v0x * v0x + v0y * v0y + v0z * v0z);
        if (v0Len > 1e-6) {
            v0x /= v0Len;
            v0y /= v0Len;
            v0z /= v0Len;
        }

        double px0 = pos0.x;
        double py0 = pos0.y;
        double pz0 = pos0.z;

        if (face || calculateNormalRaw(p0x, p0y, bs_sin0_div2, -p2x, -p2y, -z2, -p3x, -p3y, -z3, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (p0x + px0), (float) (p0y + py0), (float) (bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p2x + px0), (float) (-p2y + py0), (float) (-z2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p3x + px0), (float) (-p3y + py0), (float) (-z3 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p1x + px0), (float) (p1y + py0), (float) (bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
        }

        if (face || calculateNormalRaw(p1x, p1y, bs_cos0_div2, -p3x, -p3y, -z3, -p0x, -p0y, -bs_sin0_div2, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (p1x + px0), (float) (p1y + py0), (float) (bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p3x + px0), (float) (-p3y + py0), (float) (-z3 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p0x + px0), (float) (-p0y + py0), (float) (-bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p2x + px0), (float) (p2y + py0), (float) (z2 + pz0)).setColor(red, green, blue, alpha);
        }

        if (face || calculateNormalRaw(p2x, p2y, z2, -p0x, -p0y, -bs_sin0_div2, -p1x, -p1y, -bs_cos0_div2, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (p2x + px0), (float) (p2y + py0), (float) (z2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p0x + px0), (float) (-p0y + py0), (float) (-bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p1x + px0), (float) (-p1y + py0), (float) (-bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p3x + px0), (float) (p3y + py0), (float) (z3 + pz0)).setColor(red, green, blue, alpha);
        }

        if (face || calculateNormalRaw(p3x, p3y, z3, -p1x, -p1y, -bs_cos0_div2, -p2x, -p2y, -z2, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (p3x + px0), (float) (p3y + py0), (float) (z3 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p1x + px0), (float) (-p1y + py0), (float) (-bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p2x + px0), (float) (-p2y + py0), (float) (-z2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p0x + px0), (float) (p0y + py0), (float) (bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
        }

        if (face || calculateNormalRaw(p0x, p0y, bs_sin0_div2, p1x, p1y, bs_cos0_div2, p2x, p2y, z2, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (p0x + px0), (float) (p0y + py0), (float) (bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p1x + px0), (float) (p1y + py0), (float) (bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p2x + px0), (float) (p2y + py0), (float) (z2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (p3x + px0), (float) (p3y + py0), (float) (z3 + pz0)).setColor(red, green, blue, alpha);
        }

        if (face || calculateNormalRaw(-p0x, -p0y, -bs_sin0_div2, -p3x, -p3y, -z3, -p2x, -p2y, -z2, v0x, v0y, v0z)) {
            consumer.addVertex(pose, (float) (-p0x + px0), (float) (-p0y + py0), (float) (-bs_sin0_div2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p3x + px0), (float) (-p3y + py0), (float) (-z3 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p2x + px0), (float) (-p2y + py0), (float) (-z2 + pz0)).setColor(red, green, blue, alpha);
            consumer.addVertex(pose, (float) (-p1x + px0), (float) (-p1y + py0), (float) (-bs_cos0_div2 + pz0)).setColor(red, green, blue, alpha);
        }
    }

    public static boolean calculateNormalRaw(double p0x, double p0y, double p0z,
                                             double p1x, double p1y, double p1z,
                                             double p2x, double p2y, double p2z,
                                             double cx, double cy, double cz) {

        double dx1 = p1x - p0x;
        double dy1 = p1y - p0y;
        double dz1 = p1z - p0z;

        double dx2 = p2x - p0x;
        double dy2 = p2y - p0y;
        double dz2 = p2z - p0z;

        double nx = dy1 * dz2 - dz1 * dy2;
        double ny = dz1 * dx2 - dx1 * dz2;
        double nz = dx1 * dy2 - dy1 * dx2;

        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len < 1e-6) return false;

        nx /= len;
        ny /= len;
        nz /= len;

        double cLen = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (cLen < 1e-6) return false;
        cx /= cLen; cy /= cLen; cz /= cLen;

        double dotProduct = nx * cx + ny * cy + nz * cz;

        return dotProduct > 0;
    }
}
