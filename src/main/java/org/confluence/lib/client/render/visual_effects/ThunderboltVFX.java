package org.confluence.lib.client.render.visual_effects;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.util.VectorUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.confluence.lib.util.RenderUtils.*;
import static org.confluence.lib.util.VectorUtils.lightningPathList;

public class ThunderboltVFX extends VisualEffects {
    private final Map<Vector2d, Vector2d> POINTS_MAP = new HashMap<>();
    private final Map<Vector3d, Integer> LIGHTNING = new HashMap<>();
    private final List<Double> POINTS_SIDE = new ArrayList<>();
    private long TIME_BEFORE = 0;
    private double ROTATE0 = 0;
    private double ROTATE1 = 0;

    @Override
    public void render(Vec3 vec3, RandomSource randomSource, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {

        ROTATE0 += 0.03;
        ROTATE1 += 0.03;
        if (ROTATE0 > (Math.PI * 2)) ROTATE0 -= (Math.PI * 2);
        if (ROTATE1 > (Math.PI * 2)) ROTATE1 -= (Math.PI * 2);

        int maxCount = 5;
        double moveDis = 0.5;
        ClientLevel level = getMinecraft().level;
        final long timeVariable = (System.currentTimeMillis() / 35) % 10000;
        boolean updata = (TIME_BEFORE != timeVariable);
        TIME_BEFORE = timeVariable;
        RandomSource random = RandomSource.create();
        RandomSource random1 = RandomSource.create();
        RandomSource random2 = RandomSource.create();
        RandomSource random3 = RandomSource.create();
        random1.setSeed(timeVariable);

        random.setSeed(random1.nextInt());
        random2.setSeed(random1.nextInt());

        while (POINTS_MAP.size() < maxCount) {
            double rotate = random2.nextDouble() * 2 * Math.PI;
            double length0 = random2.nextDouble() * 5;
            double length1 = random2.nextDouble() * 35 + 20;
            Vector2d pointFirst = new Vector2d(length1 * Math.cos(rotate), length1 * Math.sin(rotate));
            Vector2d pointLast = new Vector2d(length0 * Math.cos(rotate), length0 * Math.sin(rotate));
            POINTS_MAP.put(pointFirst, pointLast);
        }

        while (POINTS_SIDE.size() < maxCount) {
            POINTS_SIDE.add(0.6 + random2.nextDouble());
        }

        Vec3 entityPos = new Vec3(0, 0, 0);
        Vector3d entityMainPos = toVector3d(vec3);

        List<Vector2d> removePoints = new ArrayList<>();
        int sideGet = 0;

        for (Map.Entry<Vector2d, Vector2d> doublePoints : POINTS_MAP.entrySet()) {
            int yMax = 0;
            boolean showTheLightning = true;
            double mainSide = POINTS_SIDE.get(sideGet);
            Vector2d pointFirst = doublePoints.getKey();
            Vector2d pointLast = doublePoints.getValue();
            if (updata) {
                double pointLastLength = pointLast.length();
                pointLast = new Vector2d(pointLast.x + (moveDis / pointLastLength * pointLast.x), pointLast.y + (moveDis / pointLastLength * pointLast.y));
                if (pointLast.length() > pointFirst.length()) {
                    removePoints.add(pointFirst);
                    POINTS_SIDE.remove(sideGet);
                    POINTS_SIDE.add(sideGet, 0.6 + random2.nextDouble());
                } else POINTS_MAP.put(pointFirst, pointLast);
            }
            BlockPos checkPos = new BlockPos((int) (pointLast.x + entityMainPos.x), (int) entityMainPos.y, (int) (pointLast.y + entityMainPos.z));
            if (level != null) {
                while (level.getBlockState(checkPos).canBeReplaced() || !level.getBlockState(checkPos.offset(0, 1, 0)).canBeReplaced()) {
                    if (level.getBlockState(checkPos).canBeReplaced() && level.getBlockState(checkPos.offset(0, 1, 0)).canBeReplaced())
                        checkPos = checkPos.offset(0, -1, 0);
                    if (!level.getBlockState(checkPos).canBeReplaced() && !level.getBlockState(checkPos.offset(0, 1, 0)).canBeReplaced())
                        checkPos = checkPos.offset(0, 1, 0);
                    int y = checkPos.getY();
                    yMax++;
                    if ((y > level.getMaxBuildHeight()) || (y < level.getMinBuildHeight()) || (yMax > 40)) {
                        showTheLightning = false;
                    }
                    if (!showTheLightning) break;
                }
            }
            if (!showTheLightning) continue;
            Vec3 targetPos = entityPos.add(pointLast.x, checkPos.getY() - entityMainPos.y, pointLast.y());
            Vector3d mPos = new Vector3d(targetPos.x / 2 + random.nextDouble() * 6 - 3, -random.nextDouble() * 6 - 2, targetPos.z / 2 + random.nextDouble() * 6 - 3);

            List<Vector3d> pathPoints = new ArrayList<>();
            pathPoints.add(toVector3d(entityPos));
            if (mPos.y > targetPos.y) pathPoints.add(mPos);
            pathPoints.add(toVector3d(targetPos));

            lightningPathList(
                    pathPoints,
                    mainSide + 1.5,
                    (float) (0.1 * mainSide),
                    random
            );
            Vector3d point0 = pathPoints.getFirst();
            Vector3d point1 = pathPoints.get(1);
            sideGet++;

            Vector3d pointF0 = pathPoints.getLast();
            Vector3d pointF1 = pathPoints.get(pathPoints.size() - 2);
            Vector3d pointN1 = new Vector3d(2 * point0.x - point1.x, 2 * point0.y - point1.y, 2 * point0.z - point1.z);
            Vector3d pointNF1 = new Vector3d(2 * pointF0.x - pointF1.x, 2 * pointF0.y - pointF1.y, 2 * pointF0.z - pointF1.z);
            pathPoints.addFirst(pointN1);
            pathPoints.add(pointNF1);
            renderLightningPath(poseStack, bufferSource, pathPoints, 180, 0, 255, 255, mainSide + 0.3, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 128, 0, 255, 127, mainSide + 0.7, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 255, 255, 255, 255, mainSide + 0.1, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 0, 0, 0, 255, mainSide, entityMainPos, true);
        }

        for (Vector2d removePoint : removePoints) {
            POINTS_MAP.remove(removePoint);
        }
        VertexConsumer consumer1 = bufferSource.getBuffer(RenderType.debugQuads());
        drawCube(poseStack, 10, 0, 0, 0, 255, entityMainPos, new Vector3d(0, 0, 0), true, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 10.1, 255, 255, 255, 255, entityMainPos, new Vector3d(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 10.3, 180, 0, 255, 255, entityMainPos, new Vector3d(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 11.5, 128, 0, 255, 127, entityMainPos, new Vector3d(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);

        while ((LIGHTNING.size() < 10) && (random2.nextDouble() < 0.6)) {
            Vector3d facing = new Vector3d(random2.nextDouble() * 2 - 1, random2.nextDouble() * 2 - 1, random2.nextDouble() * 2 - 1);
            while (facing.length() > 1)
                facing = new Vector3d(random2.nextDouble() * 2 - 1, random2.nextDouble() * 2 - 1, random2.nextDouble() * 2 - 1);
            facing.normalize().mul(random2.nextDouble() * 8 + 20);
            LIGHTNING.put(facing, 0);
        }

        List<Vector3d> willRemove = new ArrayList<>();

        int maxTime = 70;
        int debugTime = 25;

        for (Map.Entry<Vector3d, Integer> entry : LIGHTNING.entrySet()) {
            random3.setSeed((long) entry.getKey().length());
            List<Vector3d> debugList = new ArrayList<>();
            Vector3d debugVct0 = new Vector3d(new Vector3d(entry.getKey()).normalize().mul(5));
            Vector3d debugVct1 = new Vector3d(entry.getKey());
            debugList.add(debugVct0);
            debugList.add(debugVct1);
            List<List<Vector3d>> newLightning = lightningPathList(debugList, 0.2, 0.1F, random3, 3, 0.5F);
            int alpha;
            int time = entry.getValue();
            alpha = switch (time) {
                case 5, 10, 15, 20 -> 0;
                case 6, 11, 16, 21 -> 100;
                case 7, 12, 17, 22 -> 200;
                default -> 255;
            };
            if (time > debugTime)
                alpha = (int) (((double) (maxTime - time) / (double) (maxTime - debugTime)) * 255);

            for (List<Vector3d> listVct : newLightning) {
                Vector3d beforeVct = new Vector3d(0, 0, 0);
                int i = 0;
                for (Vector3d lightningVct : listVct) {
                    if (i != 0) {
                        PoseStack.Pose pose = poseStack.last();
                        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
                        poseStack.pushPose();
                        {
                            consumer.addVertex(pose, (float) beforeVct.x, (float) beforeVct.y, (float) beforeVct.z)
                                    .setColor(180, 0, 255, alpha)
                                    .setNormal(pose, 0, 1, 0);
                            consumer.addVertex(pose, (float) lightningVct.x, (float) lightningVct.y, (float) lightningVct.z)
                                    .setColor(180, 0, 255, alpha)
                                    .setNormal(pose, 0, 1, 0);
                        }
                        poseStack.popPose();
                    }
                    beforeVct = lightningVct;
                    i++;
                }
            }
            if (updata) time++;
            if (time >= maxTime) willRemove.add(entry.getKey());
            else LIGHTNING.put(entry.getKey(), time);
        }

        for (Vector3d removeKey : willRemove) {
            LIGHTNING.remove(removeKey);
        }
    }

    private void renderLightningPath(PoseStack poseStack, MultiBufferSource bufferSource, List<Vector3d> pathPoints, int red, int green, int blue, int alpha, double side, Vector3d entityPos, boolean face) {

        List<Vector3d> points = new ArrayList<>();
        List<Vector3d> points0 = new ArrayList<>();
        List<Vector3d> points1 = new ArrayList<>();
        List<Vector3d> points2 = new ArrayList<>();
        List<Vector3d> points3 = new ArrayList<>();
        Vector3d before = new Vector3d(0, 0, 0);
        Vector3d point = new Vector3d(0, 0, 0);

        int j = 0;
        for (Vector3d after : pathPoints) {
            if (j >= 2) {
                points.clear();
                VectorUtils.findVerticalPlane(point, before, after, side, points);
                while(points.size() < 4) points.add(new Vector3d(0, 0, 0));

                points0.add(points.get(0));
                points1.add(points.get(1));
                points2.add(points.get(2));
                points3.add(points.get(3));
            }
            before = point;
            point = after;
            j++;
        }

        PoseStack.Pose pose = poseStack.last();
        poseStack.pushPose();
        Vector3d rawCameraPos = toVector3d(getCamera().getPosition());
        double cx = rawCameraPos.x - entityPos.x;
        double cy = rawCameraPos.y - entityPos.y;
        double cz = rawCameraPos.z - entityPos.z;

        double cLen = Math.sqrt(cx * cx + cy * cy + cz * cz);
        if (cLen > 1e-6) {
            cx /= cLen;
            cy /= cLen;
            cz /= cLen;
        }

        for (int i = 0; i < points0.size() - 1; i++) {
            double s0x = points0.get(i).x, s0y = points0.get(i).y, s0z = points0.get(i).z;
            double e0x = points0.get(i+1).x, e0y = points0.get(i+1).y, e0z = points0.get(i+1).z;

            double s1x = points1.get(i).x, s1y = points1.get(i).y, s1z = points1.get(i).z;

            double s2x = points2.get(i).x, s2y = points2.get(i).y, s2z = points2.get(i).z;
            double e2x = points2.get(i+1).x, e2y = points2.get(i+1).y, e2z = points2.get(i+1).z;

            double s3x = points3.get(i).x, s3y = points3.get(i).y, s3z = points3.get(i).z;
            double e3x = points3.get(i+1).x, e3y = points3.get(i+1).y, e3z = points3.get(i+1).z;

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

            poseStack.pushPose();
            {
                if (face || calculateNormalRaw(s0x, s0y, s0z, e2x, e2y, e2z, e3x, e3y, e3z, cx, cy, cz)) {
                    consumer.addVertex(pose, (float)s0x, (float)s0y, (float)s0z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)e0x, (float)e0y, (float)e0z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)e3x, (float)e3y, (float)e3z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s1x, (float)s1y, (float)s1z).setColor(red, green, blue, alpha);
                }

                if (face || calculateNormalRaw(s1x, s1y, s1z, e3x, e3y, e3z, s0x, s0y, s0z, cx, cy, cz)) {
                    consumer.addVertex(pose, (float)s1x, (float)s1y, (float)s1z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)e3x, (float)e3y, (float)e3z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s0x, (float)s0y, (float)s0z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s2x, (float)s2y, (float)s2z).setColor(red, green, blue, alpha);
                }

                if (face || calculateNormalRaw(s2x, s2y, s2z, s0x, s0y, s0z, s1x, s1y, s1z, cx, cy, cz)) {
                    consumer.addVertex(pose, (float)s2x, (float)s2y, (float)s2z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s0x, (float)s0y, (float)s0z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s1x, (float)s1y, (float)s1z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)e3x, (float)e3y, (float)e3z).setColor(red, green, blue, alpha);
                }

                if (face || calculateNormalRaw(s3x, s3y, s3z, s1x, s1y, s1z, s2x, s2y, s2z, cx, cy, cz)) {
                    consumer.addVertex(pose, (float)s3x, (float)s3y, (float)s3z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s1x, (float)s1y, (float)s1z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s2x, (float)s2y, (float)s2z).setColor(red, green, blue, alpha);
                    consumer.addVertex(pose, (float)s0x, (float)s0y, (float)s0z).setColor(red, green, blue, alpha);
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
