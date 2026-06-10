package org.confluence.lib.util;

import PortLib.extensions.java.util.List.PortListExtension;
import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.*;
import java.util.function.Predicate;

public final class LibGeometryUtils {
    private static final BlockPos[] DIRECTIONS = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0),
            new BlockPos(0, 1, 0), new BlockPos(0, -1, 0),
            new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)
    };

    /// DFS包含方块检测
    ///
    /// @param center   中心方块坐标
    /// @param radius   半径
    /// @param contains 包含方块的判定条件
    /// @return 如果空间是封闭的，则返回封闭空间；否则返回空列表
    public static List<BlockPos> zoomDetection(Level world, BlockPos center, int radius, Predicate<BlockState> contains) {
        // 定义完整的边界范围
        int minX = center.getX() - radius;
        int maxX = center.getX() + radius;
        int minY = center.getY() - radius;
        int maxY = center.getY() + radius;
        int minZ = center.getZ() - radius;
        int maxZ = center.getZ() + radius;

        // 如果 center 方块不是空气，则直接返回空列表
        if (!world.getBlockState(center).isAir()) {
            return Collections.emptyList();
        }

        Set<BlockPos> visited = new HashSet<>();
        // 记录封闭空间
        List<BlockPos> closedSpace = new ArrayList<>();

        // DFS
        Stack<BlockPos> stack = new Stack<>();
        stack.push(center);
        visited.add(center);

        while (!stack.isEmpty()) {
            BlockPos currentPos = stack.pop();
            closedSpace.add(currentPos);

            // 检查邻居方块
            for (BlockPos dir : DIRECTIONS) {
                BlockPos neighborPos = currentPos.offset(dir);

                // 如果邻居方块在边界内
                if (neighborPos.getX() >= minX && neighborPos.getX() <= maxX &&
                        neighborPos.getY() >= minY && neighborPos.getY() <= maxY &&
                        neighborPos.getZ() >= minZ && neighborPos.getZ() <= maxZ
                ) {
                    // 如果邻居方块是空气且未被访问，加入栈
                    if (contains.test(world.getBlockState(neighborPos)) && !visited.contains(neighborPos)) {
                        stack.push(neighborPos);
                        visited.add(neighborPos);
                    }
                } else {
                    // 如果邻居方块在边界外，则当前空间不封闭
                    return Collections.emptyList();
                }
            }
        }

        return closedSpace;
    }

    /// 生成多層次、帶有隨機分支的閃電路徑列表
    ///
    /// @param initialLocationList 初始的閃電路徑節點列表（起點和終點即可）
    /// @param dist                相鄰兩個閃電節點之間的基礎間距（用於細分折線，決定閃電的顆粒度/鋸齒密度）
    /// @param move                節點細分時的偏移係數（控制閃電主幹的「抖動」幅度或彎曲程度，值越大越扭曲，超過0.4容易陷入不可控循環，推薦0.125比較寫實）
    /// @param random              隨機數生成器實例
    /// @param layer               閃電的遞迴/分裂層數（控制分支的層級深度，例如 1 只有主幹分出的一次分支，2 會有二級分支）
    /// @param branchPercent       分支長度係數（控制分支相對於主幹的基礎長度比例，結合隨機值決定分支到底有多長）
    /// @return 包含所有生成閃電路徑的列表（外層 List 是不同的閃電鏈，內層 List 是單條閃電的軌跡點坐標）
    public static List<List<Vector3f>> lightningPathList(List<Vector3f> initialLocationList, float dist, float move, RandomSource random, int layer, float branchPercent) {
        List<List<Vector3f>> listOfLightning = new ArrayList<>();

        List<List<Vector3f>> currentLayerPaths = new ArrayList<>();
        currentLayerPaths.add(new ArrayList<>(initialLocationList));

        for (int i = 0; i < layer; i++) {
            List<List<Vector3f>> nextLayerPaths = new ArrayList<>();

            for (List<Vector3f> path : currentLayerPaths) {
                List<Vector3f> refinedPath = new ArrayList<>(path);
                lightningPathList(refinedPath, dist, move, random);
                listOfLightning.add(refinedPath);

                if (refinedPath.size() < 2) continue;

                Vector3f vctBefore = PortListExtension.getFirst(refinedPath);
                Vector3f lastVct = PortListExtension.getLast(refinedPath);

                for (int j = 1; j < refinedPath.size(); j++) {
                    Vector3f vct = refinedPath.get(j);

                    float percent = 0.02F * (1 - ((float) j / refinedPath.size()));
                    if (random.nextFloat() < percent) {
                        Vector3f branchDirection = new Vector3f(vct).sub(vctBefore).normalize().mul(vctBefore.distance(lastVct) * (branchPercent + random.nextFloat() * 0.1F));

                        Vector3f randomOffset = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F)
                                .normalize().mul(branchDirection.length() * 0.5F);

                        Vector3f branchEnd = new Vector3f(vctBefore).add(branchDirection).add(randomOffset);

                        List<Vector3f> newBranch = new ArrayList<>();
                        newBranch.add(new Vector3f(vctBefore));
                        newBranch.add(branchEnd);
                        nextLayerPaths.add(newBranch);
                    }
                    vctBefore = vct;
                }
            }
            currentLayerPaths = nextLayerPaths;
            if (currentLayerPaths.isEmpty()) {
                break;
            }
        }

        return listOfLightning;
    }

    public static Map<Vector3f, BooleanStorage4> mazePos(Vector3f centerPos, float distance, int layer, RandomSource random, float difficulty) {
        Map<Vector3i, BooleanStorage4> nowMap = new HashMap<>();
        Map<Vector3i, BooleanStorage4> thanMap = new HashMap<>();
        Map<Vector3i, BooleanStorage4> setMap = new HashMap<>();
        Map<Vector3f, BooleanStorage4> outMap = new HashMap<>();
        thanMap.put(new Vector3i(), new BooleanStorage4());
        int maxCount = Mth.square(layer * 2 + 1);
        while (setMap.size() < maxCount) {
            nowMap.clear();
            nowMap.putAll(thanMap);
            nowMap.putAll(setMap);
            thanMap.clear();
            for (Map.Entry<Vector3i, BooleanStorage4> entry : nowMap.entrySet()) {
                Vector3i key = entry.getKey();
                int x = key.x;
                int z = key.z;
                BooleanStorage4 a = entry.getValue().copy();
                BooleanStorage4 b = a.copy();
                for (int i = 0; i < 4; i++) {
                    int xOffset = (int) Mth.cos(i * Mth.HALF_PI) + x;
                    int zOffset = (int) Mth.sin(i * Mth.HALF_PI) + z;
                    Vector3i thanKey = new Vector3i(xOffset, 0, zOffset);
                    b.set(i, true);
                    if (((1.0F - 0.5F * difficulty) > random.nextFloat()) && (xOffset <= layer) && (xOffset >= -layer) && (zOffset <= layer) && (zOffset >= -layer) && !setMap.containsKey(thanKey) && !nowMap.containsKey(thanKey) && !thanMap.containsKey(thanKey)) {
                        a.set(i, true);
                        BooleanStorage4 thenList = new BooleanStorage4();
                        thenList.set((i + 2) % 4, true);
                        thanMap.put(thanKey, thenList);
                    }
                }
                setMap.put(key, a);
            }
        }
        for (Map.Entry<Vector3i, BooleanStorage4> entry : setMap.entrySet()) {
            Vector3i key = entry.getKey();
            int x = key.x;
            int z = key.z;
            float dX = x * distance + centerPos.x;
            float dZ = z * distance + centerPos.z;
            BooleanStorage4 outList = entry.getValue().copy();
            outMap.put(new Vector3f(dX, centerPos.y, dZ), outList);
        }
        return outMap;
    }

    public static void lightningPathList(List<Vector3f> locationList, float dist, float move, RandomSource random) {
        float distSqr = dist * dist;
        boolean refined;
        do {
            refined = false;
            for (int i = 0; i < locationList.size() - 1; i++) {
                Vector3f point1 = locationList.get(i);
                Vector3f point2 = locationList.get(i + 1);
                float distanceSqr = point2.distanceSquared(point1);
                if (distanceSqr > distSqr) {
                    Vector3f midpoint = new Vector3f();
                    point1.add(point2, midpoint).mul(0.5F);
                    float offset = Mth.sqrt(distanceSqr) * move;
                    float twoOffset = offset * 2;
                    midpoint.x = midpoint.x + (random.nextFloat() - 0.5F) * twoOffset;
                    midpoint.y = midpoint.y + (random.nextFloat() - 0.5F) * twoOffset;
                    midpoint.z = midpoint.z + (random.nextFloat() - 0.5F) * twoOffset;
                    locationList.add(i + 1, midpoint);
                    refined = true;
                }
            }
        } while (refined);
    }

    public static void list8(List<Vector3f> list, BlockPos centerPos, int x, int y, int z, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            if (!LibMathUtils.checkChance(0.125F, random)) continue;
            list.add(new Vector3f(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1))));
        }
    }

    public static void list8(List<Vector3f> list, BlockPos centerPos, int x, int y, int z, RandomSource random, int checkY) {
        for (int i = 0; i < 8; i++) {
            if (!LibMathUtils.checkChance(0.125F, random)) continue;
            Vector3f pos = new Vector3f(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
            if (pos.y < checkY) {
                list.add(pos);
            }
        }
    }

    /// 生成坐标列表
    ///
    /// 生成球体坐标列表，带有随机比例
    public static List<Vector3f> ballPos(float radiusD, BlockPos centerPos, float chance, RandomSource random) {
        List<Vector3f> list = new ArrayList<>();
        int radius = (int) radiusD + 1;
        float radius2 = radiusD * radiusD;
        int x2;
        int y2;
        float chance8 = chance * 8;
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if (LibMathUtils.checkChance(chance8, random) && x2 + y2 + z * z <= radius2) {
                        list8(list, centerPos, x, y, z, random);
                    }
                }
            }
        }
        return list;
    }

    /// 生成椭球体坐标列表，带有随机比例
    public static List<Vector3f> ellipsoidPos(float radiusDX, float radiusDY, float radiusDZ, BlockPos centerPos, float chance, RandomSource random) {
        List<Vector3f> list = new ArrayList<>();
        int radiusX = Mth.ceil(radiusDX);
        int radiusY = Mth.ceil(radiusDY);
        int radiusZ = Mth.ceil(radiusDZ);
        float inv_rX = LibMathUtils.invertSquare(radiusDX);
        float inv_rY = LibMathUtils.invertSquare(radiusDY);
        float inv_rZ = LibMathUtils.invertSquare(radiusDZ);
        float chance8 = chance * 8;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if (LibMathUtils.checkChance(chance8, random) && x2 * inv_rX + y2 * inv_rY + z * z * inv_rZ <= 1) {
                        list8(list, centerPos, x, y, z, random);
                    }
                }
            }
        }
        return list;
    }

    /// 生成椭球体坐标列表，带有内径、随机比例、最大y坐标
    public static List<Vector3f> ellipsoidPos(float radiusDXIn, float radiusDYIn, float radiusDZIn, float radiusDXOut, float radiusDYOut, float radiusDZOut, BlockPos centerPos, float chance, RandomSource random, int checkY) {
        List<Vector3f> list = new ArrayList<>();
        int radiusX = Mth.ceil(radiusDXOut);
        int radiusY = Mth.ceil(radiusDYOut);
        int radiusZ = Mth.ceil(radiusDZOut);
        float inv_rXOut = LibMathUtils.invertSquare(radiusDXOut);
        float inv_rYOut = LibMathUtils.invertSquare(radiusDYOut);
        float inv_rZOut = LibMathUtils.invertSquare(radiusDZOut);
        float inv_rXIn = LibMathUtils.invertSquare(radiusDXIn);
        float inv_rYIn = LibMathUtils.invertSquare(radiusDYIn);
        float inv_rZIn = LibMathUtils.invertSquare(radiusDZIn);
        float chance8 = chance * 8;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    int z2 = z * z;
                    if (LibMathUtils.checkChance(chance8, random) &&
                            x2 * inv_rXOut + y2 * inv_rYOut + z2 * inv_rZOut <= 1 &&
                            x2 * inv_rXIn + y2 * inv_rYIn + z2 * inv_rZIn >= 1
                    ) {
                        list8(list, centerPos, x, y, z, random, checkY);
                    }
                }
            }
        }
        return list;
    }

    /// 生成螺旋形坐标列表
    public static List<Vector3f> rotateCloudPos(float rotate, float rotateStep, float length, float lengthStep, int count, BlockPos centerPos) {
        List<Vector3f> poses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            poses.add(new Vector3f(
                    centerPos.getX() + (length + lengthStep * i) * Mth.cos(rotate + rotateStep * i),
                    centerPos.getY(),
                    centerPos.getZ() + (length + lengthStep * i) * Mth.sin(rotate + rotateStep * i)
            ));
        }
        return poses;
    }

    /// 生成环形坐标列表
    public static void roundPos(BlockPos centerPos, float radius, RandomSource random, List<Vector3f> list, int offset, int rotate, float start) {
        float rStep = Mth.TWO_PI / rotate;
        for (int i = 0; i < rotate; i++) {
            list.add(LibMathUtils.toVector3f(centerPos.offset(
                    Mth.floor(Mth.cos(rStep * i + start) * radius) + random.nextInt(-offset, offset + 1),
                    0,
                    Mth.floor(Mth.sin(rStep * i + start) * radius) + random.nextInt(-offset, offset + 1)
            )));
        }
    }

    /// 生成任意角度圆台坐标列表
    public static List<Vector3f> frustumSetPos(Vector3f startPos, Vector3f endPos, float startRadius, float endRadius, float chance, RandomSource random) {
        int xStart0 = (int) (startPos.x + startRadius + 1);
        int xStart1 = (int) (startPos.x - startRadius - 1);
        int xEnd0 = (int) (endPos.x + endRadius + 1);
        int xEnd1 = (int) (endPos.x - endRadius - 1);
        int yStart0 = (int) (startPos.y + startRadius + 1);
        int yStart1 = (int) (startPos.y - startRadius - 1);
        int yEnd0 = (int) (endPos.y + endRadius + 1);
        int yEnd1 = (int) (endPos.y - endRadius - 1);
        int zStart0 = (int) (startPos.z + startRadius + 1);
        int zStart1 = (int) (startPos.z - startRadius - 1);
        int zEnd0 = (int) (endPos.z + endRadius + 1);
        int zEnd1 = (int) (endPos.z - endRadius - 1);

        int setStartX = Math.min(xStart1, xEnd1);
        int setEndX = Math.max(xStart0, xEnd0);
        int setStartY = Math.min(yStart1, yEnd1);
        int setEndY = Math.max(yStart0, yEnd0);
        int setStartZ = Math.min(zStart1, zEnd1);
        int setEndZ = Math.max(zStart0, zEnd0);

        float inv_length = 1 / startPos.distance(endPos);

        List<Vector3f> list = new ArrayList<>();

        for (int x = setStartX; x <= setEndX; x++) {
            for (int y = setStartY; y <= setEndY; y++) {
                for (int z = setStartZ; z <= setEndZ; z++) {
                    if (chance > random.nextFloat()) {
                        Vector3f pointP = new Vector3f(x, y, z);
                        if (!LibMathUtils.isProjectionBetweenPoints(startPos, endPos, pointP)) continue;
                        Vector3f pointP2 = LibMathUtils.getProjectionOnLineSegment(startPos, endPos, pointP);
                        float lengthGet = pointP2.distance(endPos);//0;//Math.sqrt(y2 + Mth.square(endPos.z - z) - getDistanceToLineSegment(startPos, endPos, pointP));
                        float lengthP = lengthGet * inv_length;
                        if (pointP.distance(pointP2) <= startRadius * lengthP + endRadius * (1.0D - lengthP)) {
                            list.add(pointP);
                        }
                    }
                }
            }
        }
        return list;
    }

    /// 立方体坐标列表，带有随机比例
    public static List<Vector3f> rectangularPos(BlockPos startPos, BlockPos endPos, float chance, RandomSource random) {
        int startX = Math.min(endPos.getX(), startPos.getX());
        int startY = Math.min(endPos.getY(), startPos.getY());
        int startZ = Math.min(endPos.getZ(), startPos.getZ());
        int endX = Math.max(endPos.getX(), startPos.getX());
        int endY = Math.max(endPos.getY(), startPos.getY());
        int endZ = Math.max(endPos.getZ(), startPos.getZ());
        int xLength = endX - startX;
        int yLength = endY - startY;
        int zLength = endZ - startZ;
        List<Vector3f> list = new ArrayList<>();
        for (int x = 0; x <= xLength; x++) {
            for (int y = 0; y <= yLength; y++) {
                for (int z = 0; z <= zLength; z++) {
                    if (!LibMathUtils.checkChance(chance, random)) continue;
                    list.add(new Vector3f(startX + x, startY + y, startZ + z));
                }
            }
        }
        return list;
    }

    public static void findVerticalPlane(Vector3f point, Vector3f before, Vector3f after, float side, List<Vector3f> returnList) {
        float l = point.distance(after) / point.distance(before);

        float hSide = side * 0.5F;

        float x0 = ((point.x - before.x) * l + (after.x - point.x)) * 0.5F;
        float y0 = ((point.y - before.y) * l + (after.y - point.y)) * 0.5F;
        float z0 = ((point.z - before.z) * l + (after.z - point.z)) * 0.5F;

        float[] nX = new float[3];
        float[] nZ = new float[3];
        float lToY2 = (float) Mth.length(x0, y0);

        if (x0 != 0) {
            nX = new float[]{y0 / lToY2, -x0 / lToY2, 0};
            float inv_lX = 1 / LibMathUtils.length(nX);
            nX[0] = nX[0] * inv_lX;
            nX[1] = nX[1] * inv_lX;
            nX[2] = nX[2] * inv_lX;
        }
        if (z0 != 0) {
            float inv_l2 = (float) (1 / Mth.length(x0, y0, z0));
            nZ = new float[]{-z0 * x0 * inv_l2 / lToY2, -z0 * y0 * inv_l2 / lToY2, lToY2 * inv_l2};
            float inv_lZ = 1 / LibMathUtils.length(nZ);
            nZ[0] *= inv_lZ;
            nZ[1] *= inv_lZ;
            nZ[2] *= inv_lZ;
        }

        returnList.add(new Vector3f((nX[0] + nZ[0]) * hSide + point.x, (nX[1] + nZ[1]) * hSide + point.y, (nX[2] + nZ[2]) * hSide + point.z));
        returnList.add(new Vector3f((nX[0] - nZ[0]) * hSide + point.x, (nX[1] - nZ[1]) * hSide + point.y, (nX[2] - nZ[2]) * hSide + point.z));
        returnList.add(new Vector3f((-nX[0] - nZ[0]) * hSide + point.x, (-nX[1] - nZ[1]) * hSide + point.y, (-nX[2] - nZ[2]) * hSide + point.z));
        returnList.add(new Vector3f((-nX[0] + nZ[0]) * hSide + point.x, (-nX[1] + nZ[1]) * hSide + point.y, (-nX[2] + nZ[2]) * hSide + point.z));
    }

    /// 凸包的内部點集采樣
    public static List<BlockPos> getBlocksInConvexHull(@Nullable List<Vector3f> points) {
        List<BlockPos> result = new ArrayList<>();
        if (points == null || points.size() < 4) return result;

        float cx = 0, cy = 0, cz = 0;
        for (Vector3f p : points) {
            cx += p.x;
            cy += p.y;
            cz += p.z;
        }
        float inv_size = (float) (1.0 / points.size());
        cx *= inv_size;
        cy *= inv_size;
        cz *= inv_size;

        int pivotIdx = 0;
        float maxDistSq = 0;
        for (int i = 0; i < points.size(); i++) {
            float d = points.get(i).distanceSquared(cx, cy, cz);
            if (d > maxDistSq) {
                maxDistSq = d;
                pivotIdx = i;
            }
        }
        Vector3f pivot = points.get(pivotIdx);

        int p1Idx = -1;
        maxDistSq = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx) continue;
            float d = points.get(i).distanceSquared(pivot);
            if (d > maxDistSq) {
                maxDistSq = d;
                p1Idx = i;
            }
        }
        if (p1Idx == -1) return result;
        Vector3f p1 = points.get(p1Idx);

        int p2Idx = -1;
        float maxArea = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx) continue;
            float area = triArea(pivot, p1, points.get(i));
            if (area > maxArea) {
                maxArea = area;
                p2Idx = i;
            }
        }
        if (p2Idx == -1) return result;
        Vector3f p2 = points.get(p2Idx);

        int p3Idx = -1;
        float maxDist = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx || i == p2Idx) continue;
            float d = pointToPlaneDist(points.get(i), pivot, p1, p2);
            if (Math.abs(d) > Math.abs(maxDist)) {
                maxDist = d;
                p3Idx = i;
            }
        }
        if (p3Idx == -1) return result;
        if (Math.abs(maxDist) < 1e-7) return result;
        Vector3f p3 = points.get(p3Idx);

        cx = (pivot.x + p1.x + p2.x + p3.x) * 0.25F;
        cy = (pivot.y + p1.y + p2.y + p3.y) * 0.25F;
        cz = (pivot.z + p1.z + p2.z + p3.z) * 0.25F;

        int[][] faces = maxDist > 0 ? new int[][]{
                {pivotIdx, p1Idx, p2Idx},
                {pivotIdx, p2Idx, p3Idx},
                {pivotIdx, p3Idx, p1Idx},
                {p1Idx, p3Idx, p2Idx}
        } : new int[][]{
                {pivotIdx, p2Idx, p1Idx},
                {pivotIdx, p1Idx, p3Idx},
                {pivotIdx, p3Idx, p2Idx},
                {p1Idx, p2Idx, p3Idx}
        };

        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx || i == p2Idx || i == p3Idx) continue;

            Vector3f point = points.get(i);
            List<int[]> newFaces = new ArrayList<>();
            LongSet boundaryEdges = new LongArraySet();

            for (int[] face : faces) {
                if (isPointOutside(point, points.get(face[0]), points.get(face[1]), points.get(face[2]), cx, cy, cz)) {
                    addEdge(boundaryEdges, face[0], face[1]);
                    addEdge(boundaryEdges, face[1], face[2]);
                    addEdge(boundaryEdges, face[2], face[0]);
                } else {
                    newFaces.add(face);
                }
            }

            if (boundaryEdges.isEmpty()) continue;

            for (long edge : boundaryEdges) {
                int a = (int) (edge >> 32);
                int b = (int) edge;
                newFaces.add(new int[]{a, b, i});
            }
            faces = newFaces.toArray(int[][]::new);
        }

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (Vector3f p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z);
            maxZ = Math.max(maxZ, p.z);
        }

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        int ceilX = Mth.ceil(maxX);
        int ceilY = Mth.ceil(maxY);
        int ceilZ = Mth.ceil(maxZ);
        for (int x = Mth.floor(minX); x <= ceilX; x++) {
            mPos.setX(x);
            for (int y = Mth.floor(minY); y <= ceilY; y++) {
                mPos.setY(y);
                label:
                for (int z = Mth.floor(minZ); z <= ceilZ; z++) {
                    float px = x + 0.5F, py = y + 0.5F, pz = z + 0.5F;
                    for (int[] face : faces) {
                        Vector3f a = points.get(face[0]), b = points.get(face[1]), c = points.get(face[2]);

                        float[] normal = getNormal(a, b, c);
                        float nx = normal[0], ny = normal[1], nz = normal[2];

                        float fcx = (a.x + b.x + c.x) * 0.3333333F;
                        float fcy = (a.y + b.y + c.y) * 0.3333333F;
                        float fcz = (a.z + b.z + c.z) * 0.3333333F;
                        if (nx * (cx - fcx) + ny * (cy - fcy) + nz * (cz - fcz) > 0) {
                            nx = -nx;
                            ny = -ny;
                            nz = -nz;
                        }

                        if ((px - a.x) * nx + (py - a.y) * ny + (pz - a.z) * nz > 1e-7) {
                            continue label;
                        }
                    }
                    result.add(mPos.setZ(z).immutable());
                }
            }
        }
        return result;
    }

    private static float[] getNormal(Vector3f a, Vector3f b, Vector3f c) {
        float abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        float acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;
        return new float[]{nx, ny, nz};
    }

    private static void addEdge(LongSet boundary, int a, int b) {
        long reverseCode = ((long) b << 32) | a;

        if (boundary.contains(reverseCode)) {
            boundary.remove(reverseCode);
        } else {
            boundary.add(((long) a << 32) | b);
        }
    }

    private static float triArea(Vector3f a, Vector3f b, Vector3f c) {
        float abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        float acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        float nx = aby * acz - abz * acy;
        float ny = abz * acx - abx * acz;
        float nz = abx * acy - aby * acx;
        return (float) Mth.length(nx, ny, nz);
    }

    private static float pointToPlaneDist(Vector3f p, Vector3f a, Vector3f b, Vector3f c) {
        float[] normal = getNormal(a, b, c);
        return (p.x - a.x) * normal[0] + (p.y - a.y) * normal[1] + (p.z - a.z) * normal[2];
    }

    private static boolean isPointOutside(Vector3f p, Vector3f a, Vector3f b, Vector3f c, float cx, float cy, float cz) {
        float[] normal = getNormal(a, b, c);
        float nx = normal[0], ny = normal[1], nz = normal[2];

        float fcx = (a.x + b.x + c.x) * 0.3333333F;
        float fcy = (a.y + b.y + c.y) * 0.3333333F;
        float fcz = (a.z + b.z + c.z) * 0.3333333F;

        if (nx * (cx - fcx) + ny * (cy - fcy) + nz * (cz - fcz) > 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }

        return (p.x - a.x) * nx + (p.y - a.y) * ny + (p.z - a.z) * nz > 1e-7;
    }
}
