package org.confluence.lib.client.render.visual_effects;

import PortLib.extensions.com.mojang.blaze3d.vertex.VertexConsumer.PortVertexConsumerExtension;
import PortLib.extensions.java.util.List.PortListExtension;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.util.LibGeometryUtils;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.confluence.lib.util.LibGeometryUtils.lightningPathList;
import static org.confluence.lib.util.LibRenderUtils.calculateNormalRaw;
import static org.confluence.lib.util.LibRenderUtils.drawCube;

public class ThunderboltVFX extends VisualEffects {
    private final Map<Vector2f, Vector2f> POINTS_MAP = new HashMap<>();
    private final Object2IntMap<Vector3f> LIGHTNING = new Object2IntOpenHashMap<>();
    private final FloatList POINTS_SIDE = new FloatArrayList();
    private long TIME_BEFORE = 0;
    private float ROTATE0 = 0;
    private float ROTATE1 = 0;

    @Override
    public void render(Vec3 vec3, RandomSource randomSource, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        ROTATE0 += 0.03F;
        ROTATE1 += 0.03F;
        if (ROTATE0 > Mth.TWO_PI) ROTATE0 -= Mth.TWO_PI;
        if (ROTATE1 > Mth.TWO_PI) ROTATE1 -= Mth.TWO_PI;

        int maxCount = 5;
        float moveDis = 0.5F;
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
            float rotate = random2.nextFloat() * Mth.TWO_PI;
            float length0 = random2.nextFloat() * 5;
            float length1 = random2.nextFloat() * 35 + 20;
            Vector2f pointFirst = new Vector2f(length1 * Mth.cos(rotate), length1 * Mth.sin(rotate));
            Vector2f pointLast = new Vector2f(length0 * Mth.cos(rotate), length0 * Mth.sin(rotate));
            POINTS_MAP.put(pointFirst, pointLast);
        }

        while (POINTS_SIDE.size() < maxCount) {
            POINTS_SIDE.add(0.6F + random2.nextFloat());
        }

        Vec3 entityPos = new Vec3(0, 0, 0);
        Vector3f entityMainPos = new Vector3f((float) vec3.x, (float) vec3.y, (float) vec3.z);

        List<Vector2f> removePoints = new ArrayList<>();
        int sideGet = 0;

        for (Map.Entry<Vector2f, Vector2f> floatPoints : POINTS_MAP.entrySet()) {
            int yMax = 0;
            boolean showTheLightning = true;
            float mainSide = POINTS_SIDE.getFloat(sideGet);
            Vector2f pointFirst = floatPoints.getKey();
            Vector2f pointLast = floatPoints.getValue();
            if (updata) {
                float pointLastLength = pointLast.length();
                pointLast = new Vector2f(pointLast.x + (moveDis / pointLastLength * pointLast.x), pointLast.y + (moveDis / pointLastLength * pointLast.y));
                if (pointLast.length() > pointFirst.length()) {
                    removePoints.add(pointFirst);
                    POINTS_SIDE.removeFloat(sideGet);
                    POINTS_SIDE.add(sideGet, 0.6F + random2.nextFloat());
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
            Vector3f mPos = new Vector3f((float) targetPos.x* 0.5F + random.nextFloat() * 6 - 3, -random.nextFloat() * 6 - 2, (float)targetPos.z* 0.5F + random.nextFloat() * 6 - 3);

            List<Vector3f> pathPoints = new ArrayList<>();
            pathPoints.add(entityPos.toVector3f());
            if (mPos.y > targetPos.y) pathPoints.add(mPos);
            pathPoints.add(targetPos.toVector3f());

            lightningPathList(
                    pathPoints,
                    mainSide + 1.5F,
                    0.1F * mainSide,
                    random
            );
            Vector3f point0 = PortListExtension.getFirst(pathPoints);
            Vector3f point1 = pathPoints.get(1);
            sideGet++;

            Vector3f pointF0 = PortListExtension.getLast(pathPoints);
            Vector3f pointF1 = pathPoints.get(pathPoints.size() - 2);
            Vector3f pointN1 = new Vector3f(2 * point0.x - point1.x, 2 * point0.y - point1.y, 2 * point0.z - point1.z);
            Vector3f pointNF1 = new Vector3f(2 * pointF0.x - pointF1.x, 2 * pointF0.y - pointF1.y, 2 * pointF0.z - pointF1.z);
            PortListExtension.addFirst(pathPoints, pointN1);
            pathPoints.add(pointNF1);
            renderLightningPath(poseStack, bufferSource, pathPoints, 180, 0, 255, 255, mainSide + 0.3F, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 128, 0, 255, 127, mainSide + 0.7F, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 255, 255, 255, 255, mainSide + 0.1F, entityMainPos, false);
            renderLightningPath(poseStack, bufferSource, pathPoints, 0, 0, 0, 255, mainSide, entityMainPos, true);
        }

        for (Vector2f removePoint : removePoints) {
            POINTS_MAP.remove(removePoint);
        }
        VertexConsumer consumer1 = bufferSource.getBuffer(RenderType.debugQuads());
        drawCube(poseStack, 10F, 0, 0, 0, 255, entityMainPos, new Vector3f(0, 0, 0), true, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 10.1F, 255, 255, 255, 255, entityMainPos, new Vector3f(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 10.3F, 180, 0, 255, 255, entityMainPos, new Vector3f(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);
        drawCube(poseStack, 11.5F, 128, 0, 255, 127, entityMainPos, new Vector3f(0, 0, 0), false, ROTATE0, ROTATE1, consumer1);

        while ((LIGHTNING.size() < 10) && (random2.nextFloat() < 0.6)) {
            Vector3f facing = new Vector3f(random2.nextFloat() * 2 - 1, random2.nextFloat() * 2 - 1, random2.nextFloat() * 2 - 1);
            while (facing.length() > 1)
                facing = new Vector3f(random2.nextFloat() * 2 - 1, random2.nextFloat() * 2 - 1, random2.nextFloat() * 2 - 1);
            facing.normalize().mul(random2.nextFloat() * 8 + 20);
            LIGHTNING.put(facing, 0);
        }

        List<Vector3f> willRemove = new ArrayList<>();

        int maxTime = 70;
        int debugTime = 25;

        for (Object2IntMap.Entry<Vector3f> entry : LIGHTNING.object2IntEntrySet()) {
            random3.setSeed((long) entry.getKey().length());
            List<Vector3f> debugList = new ArrayList<>();
            Vector3f debugVct0 = new Vector3f(entry.getKey()).normalize().mul(5);
            Vector3f debugVct1 = new Vector3f(entry.getKey());
            debugList.add(debugVct0);
            debugList.add(debugVct1);
            List<List<Vector3f>> newLightning = lightningPathList(debugList, 0.2F, 0.1F, random3, 3, 0.5F);
            int alpha;
            int time = entry.getIntValue();
            alpha = switch (time) {
                case 5, 10, 15, 20 -> 0;
                case 6, 11, 16, 21 -> 100;
                case 7, 12, 17, 22 -> 200;
                default -> 255;
            };
            if (time > debugTime)
                alpha = (int) (((float) (maxTime - time) / (float) (maxTime - debugTime)) * 255);

            for (List<Vector3f> listVct : newLightning) {
                Vector3f beforeVct = new Vector3f(0, 0, 0);
                int i = 0;
                for (Vector3f lightningVct : listVct) {
                    if (i != 0) {
                        PoseStack.Pose pose = poseStack.last();
                        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
                        poseStack.pushPose();
                        {
                            int finalAlpha = alpha;
                            PortVertexConsumerExtension.vertex(consumer, pose, beforeVct.x, beforeVct.y, beforeVct.z, v -> {
                                PortVertexConsumerExtension.setColor(v, 180, 0, 255, finalAlpha);
                                PortVertexConsumerExtension.setNormal(v, pose, 0, 1, 0);
                            });
                            PortVertexConsumerExtension.vertex(consumer, pose, lightningVct.x, lightningVct.y, lightningVct.z, v -> {
                                PortVertexConsumerExtension.setColor(v, 180, 0, 255, finalAlpha);
                                PortVertexConsumerExtension.setNormal(v, pose, 0, 1, 0);
                            });
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

        for (Vector3f removeKey : willRemove) {
            LIGHTNING.removeInt(removeKey);
        }
    }

    private void renderLightningPath(PoseStack poseStack, MultiBufferSource bufferSource, List<Vector3f> pathPoints, int red, int green, int blue, int alpha, float side, Vector3f entityPos, boolean face) {

        List<Vector3f> points = new ArrayList<>();
        List<Vector3f> points0 = new ArrayList<>();
        List<Vector3f> points1 = new ArrayList<>();
        List<Vector3f> points2 = new ArrayList<>();
        List<Vector3f> points3 = new ArrayList<>();
        Vector3f before = new Vector3f(0, 0, 0);
        Vector3f point = new Vector3f(0, 0, 0);

        int j = 0;
        for (Vector3f after : pathPoints) {
            if (j >= 2) {
                points.clear();
                LibGeometryUtils.findVerticalPlane(point, before, after, side, points);
                while (points.size() < 4) points.add(new Vector3f(0, 0, 0));

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
        Vec3 camPos = getCamera().getPosition();
        float cx = (float) (camPos.x - entityPos.x);
        float cy = (float) (camPos.y - entityPos.y);
        float cz = (float) (camPos.z - entityPos.z);

        float cLen = Mth.sqrt(cx * cx + cy * cy + cz * cz);
        if (cLen > 1e-6F) {
            cx /= cLen;
            cy /= cLen;
            cz /= cLen;
        }

        for (int i = 0; i < points0.size() - 1; i++) {
            float s0x = points0.get(i).x, s0y = points0.get(i).y, s0z = points0.get(i).z;
            float e0x = points0.get(i + 1).x, e0y = points0.get(i + 1).y, e0z = points0.get(i + 1).z;

            float s1x = points1.get(i).x, s1y = points1.get(i).y, s1z = points1.get(i).z;

            float s2x = points2.get(i).x, s2y = points2.get(i).y, s2z = points2.get(i).z;
            float e2x = points2.get(i + 1).x, e2y = points2.get(i + 1).y, e2z = points2.get(i + 1).z;

            float s3x = points3.get(i).x, s3y = points3.get(i).y, s3z = points3.get(i).z;
            float e3x = points3.get(i + 1).x, e3y = points3.get(i + 1).y, e3z = points3.get(i + 1).z;

            VertexConsumer consumer = bufferSource.getBuffer(RenderType.debugQuads());

            poseStack.pushPose();
            {
                if (face || calculateNormalRaw(s0x, s0y, s0z, e2x, e2y, e2z, e3x, e3y, e3z, cx, cy, cz)) {
                    PortVertexConsumerExtension.vertex(consumer, pose, s0x, s0y, s0z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, e0x, e0y, e0z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, e3x, e3y, e3z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s1x, s1y, s1z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                }

                if (face || calculateNormalRaw(s1x, s1y, s1z, e3x, e3y, e3z, s0x, s0y, s0z, cx, cy, cz)) {
                    PortVertexConsumerExtension.vertex(consumer, pose, s1x, s1y, s1z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, e3x, e3y, e3z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s0x, s0y, s0z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s2x, s2y, s2z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                }

                if (face || calculateNormalRaw(s2x, s2y, s2z, s0x, s0y, s0z, s1x, s1y, s1z, cx, cy, cz)) {
                    PortVertexConsumerExtension.vertex(consumer, pose, s2x, s2y, s2z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s0x, s0y, s0z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s1x, s1y, s1z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, e3x, e3y, e3z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                }

                if (face || calculateNormalRaw(s3x, s3y, s3z, s1x, s1y, s1z, s2x, s2y, s2z, cx, cy, cz)) {
                    PortVertexConsumerExtension.vertex(consumer, pose, s3x, s3y, s3z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s1x, s1y, s1z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s2x, s2y, s2z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                    PortVertexConsumerExtension.vertex(consumer, pose, s0x, s0y, s0z, v -> PortVertexConsumerExtension.setColor(v, red, green, blue, alpha));
                }
            }
            poseStack.popPose();
        }
        poseStack.popPose();
    }
}
