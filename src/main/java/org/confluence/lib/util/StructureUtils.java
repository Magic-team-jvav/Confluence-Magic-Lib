package org.confluence.lib.util;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;

import static org.confluence.lib.util.VectorUtils.*;

public final class StructureUtils {
    public static void ball8(boolean replace, int x, int y, int z, int blockState, BlockPos centerPos, Object2IntMap<BlockPos> blockMap) {
        for (int i = 0; i < 8; i++) {
            BlockPos blockPos = new BlockPos(centerPos.getX() + x * (i < 4 ? 1 : -1), centerPos.getY() + y * (i % 4 < 2 ? 1 : -1), centerPos.getZ() + z * (i % 2 < 1 ? 1 : -1));
            if (replace || !blockMap.containsKey(blockPos)) {
                blockMap.put(blockPos, blockState);
            }
        }
    }

    public static void ball8(boolean replace, int x, int y, int z, int blockState1, int blockState2, BlockPos centerPos, Object2IntMap<BlockPos> blockMap, int checkY) {
        for (int i = 0; i < 8; i++) {
            BlockPos blockPos = new BlockPos(centerPos.getX() + x * (i < 4 ? 1 : -1), centerPos.getY() + y * (i % 4 < 2 ? 1 : -1), centerPos.getZ() + z * (i % 2 < 1 ? 1 : -1));
            if (replace || !blockMap.containsKey(blockPos)) {
                if (blockPos.getY() > checkY) {
                    blockMap.put(blockPos, blockState1);
                } else {
                    blockMap.put(blockPos, blockState2);
                }
            }
        }
    }

    public static void ball8(boolean replace, int x, int y, int z, int blockState, BlockPos centerPos, Object2IntMap<BlockPos> blockMap, float placePer, WorldgenRandom random) {
        for (int i = 0; i < 8; i++) {
            if (placePer >= random.nextFloat()) {
                BlockPos blockPos = new BlockPos(centerPos.getX() + x * (i < 4 ? 1 : -1), centerPos.getY() + y * (i % 4 < 2 ? 1 : -1), centerPos.getZ() + z * (i % 2 < 1 ? 1 : -1));
                if (replace || !blockMap.containsKey(blockPos)) {
                    blockMap.put(blockPos, blockState);
                }
            }
        }
    }

    //填充方法
    //球体填充
    public static void ball(double radiusD, BlockPos centerPos, int blockState, boolean replace, Object2IntMap<BlockPos> blockMap) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        for (int x = 0; x < radius; x++) {
            int x2 = x * x;
            for (int y = 0; y < radius; y++) {
                int y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(replace, x, y, z, blockState, centerPos, blockMap);
                    }
                }
            }
        }
    }

    //球体填充，带有指定y坐标上下不同种方块填充
    public static void ball(double radiusD, BlockPos centerPos, int blockState1, int blockState2, boolean replace, Object2IntMap<BlockPos> blockMap, int checkY) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        for (int x = 0; x < radius; x++) {
            int x2 = x * x;
            for (int y = 0; y < radius; y++) {
                int y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(replace, x, y, z, blockState1, blockState2, centerPos, blockMap, checkY);
                    }
                }
            }
        }
    }

    //球体填充，带有随机比例
    public static void ball(double radiusD, BlockPos centerPos, int blockState, boolean replace, Object2IntMap<BlockPos> blockMap, float placePer, WorldgenRandom random) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        for (int x = 0; x < radius; x++) {
            int x2 = x * x;
            for (int y = 0; y < radius; y++) {
                int y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(replace, x, y, z, blockState, centerPos, blockMap, placePer, random);
                    }
                }
            }
        }
    }

    //椭球体填充
    public static void ellipsoid(double radiusDX, double radiusDY, double radiusDZ, BlockPos centerPos, int blockState, boolean replace, Object2IntMap<BlockPos> blockMap) {
        int radiusX = (int) radiusDX + 1;
        int radiusY = (int) radiusDY + 1;
        int radiusZ = (int) radiusDZ + 1;
        double rX = radiusDX * radiusDX;
        double rY = radiusDY * radiusDY;
        double rZ = radiusDZ * radiusDZ;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if ((x2 / rX + y2 / rY + (z * z) / rZ) <= 1) {
                        ball8(replace, x, y, z, blockState, centerPos, blockMap);
                    }
                }
            }
        }
    }

    //椭球体填充，带有指定y坐标上下不同种方块填充
    public static void ellipsoid(double radiusDX, double radiusDY, double radiusDZ, BlockPos centerPos, int blockState1, int blockState2, boolean replace, Object2IntMap<BlockPos> blockMap, int checkY) {
        int radiusX = (int) radiusDX + 1;
        int radiusY = (int) radiusDY + 1;
        int radiusZ = (int) radiusDZ + 1;
        double rX = radiusDX * radiusDX;
        double rY = radiusDY * radiusDY;
        double rZ = radiusDZ * radiusDZ;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if ((x2 / rX + y2 / rY + (z * z) / rZ) <= 1) {
                        ball8(replace, x, y, z, blockState1, blockState2, centerPos, blockMap, checkY);
                    }
                }
            }
        }
    }

    //椭球体填充，带有随机比例
    public static void ellipsoid(double radiusDX, double radiusDY, double radiusDZ, BlockPos centerPos, int blockState, boolean replace, Object2IntMap<BlockPos> blockMap, float placePer, WorldgenRandom random) {
        int radiusX = (int) radiusDX + 1;
        int radiusY = (int) radiusDY + 1;
        int radiusZ = (int) radiusDZ + 1;
        double rX = radiusDX * radiusDX;
        double rY = radiusDY * radiusDY;
        double rZ = radiusDZ * radiusDZ;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if ((x2 / rX + y2 / rY + (z * z) / rZ) <= 1) {
                        ball8(replace, x, y, z, blockState, centerPos, blockMap, placePer, random);
                    }
                }
            }
        }
    }

    //立方体填充
    public static void rectangular(BlockPos startPos, BlockPos endPos, int blockstate, Object2IntMap<BlockPos> blockMap, int replace) {
        int startX = Math.min(endPos.getX(), startPos.getX());
        int startY = Math.min(endPos.getY(), startPos.getY());
        int startZ = Math.min(endPos.getZ(), startPos.getZ());
        int endX = Math.max(endPos.getX(), startPos.getX());
        int endY = Math.max(endPos.getY(), startPos.getY());
        int endZ = Math.max(endPos.getZ(), startPos.getZ());
        int xLength = endX - startX;
        int yLength = endY - startY;
        int zLength = endZ - startZ;
        for (int x = 0; x <= xLength; x++) {
            for (int y = 0; y <= yLength; y++) {
                for (int z = 0; z <= zLength; z++) {
                    BlockPos blockPos = new BlockPos(startX + x, startY + y, startZ + z);
                    if (replace == 0) {
                        blockMap.put(blockPos, blockstate);
                    } else if (replace == 1 && blockMap.containsKey(blockPos)) {
                        blockMap.put(blockPos, blockstate);
                    } else if (replace == 2 && !blockMap.containsKey(blockPos)) {
                        blockMap.put(blockPos, blockstate);
                    }
                }
            }
        }
    }

    //任意角度圆台填充
    public static void frustumSet(Vector3d startPos, Vector3d endPos, double startRadius, double endRadius, int blockstate, Object2IntMap<BlockPos> blockMap) {
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

        Vector3d pointP = new Vector3d();
        Vector3d pointP2 = new Vector3d();
        double invLength = 1 / startPos.distance(endPos);

        for (int x = setStartX; x <= setEndX; x++) {
            for (int y = setStartY; y <= setEndY; y++) {
                for (int z = setStartZ; z <= setEndZ; z++) {
                    pointP.set(x, y, z);
                    if (!isProjectionBetweenPoints(startPos, endPos, pointP)) continue;
                    pointP2.set(getProjectionOnLineSegment(startPos, endPos, pointP));
                    double lengthGet = pointP2.distance(endPos);//0;//Math.sqrt(y2 + Mth.square(endPos.z - z) - getDistanceToLineSegment(startPos, endPos, pointP));
                    double lengthP = lengthGet * invLength;
                    if (pointP.distance(pointP2) <= (startRadius * lengthP + endRadius * (1.0D - lengthP))) {
                        blockMap.put(new BlockPos(x, y, z), blockstate);
                    }
                }
            }
        }
    }

    //金字塔填充
    public static void pyramidSet(BlockPos centerPos, int blockstate, int layerCount, Object2IntMap<BlockPos> blockMap) {
        for (int i = 0; i < layerCount; i++) {
            rectangular(centerPos.offset(layerCount - i, i, layerCount - i), centerPos.offset(i - layerCount, i, i - layerCount), blockstate, blockMap, 0);
        }
    }

    //迷宫填充
    public static void mazeSet(BlockPos centerPos, double distance, int layer, int blockstate, int width, int height, WorldgenRandom random, float difficulty, Object2IntMap<BlockPos> blockMap) {
        Map<Vector3d, BooleanStorage4> mazePos = mazePos(VectorUtils.toVector3d(centerPos), distance, layer, random, difficulty);
        int length = (int) (distance / 2) + 1;
        for (Map.Entry<Vector3d, BooleanStorage4> entry : mazePos.entrySet()) {
            BlockPos keySet = VectorUtils.fromVector3d(entry.getKey());
            BooleanStorage4 value = entry.getValue();
            if (value.get(0)) rectangular(keySet.offset(-width, 0, -width), keySet.offset(length, height, width), blockstate, blockMap, 0);
            if (value.get(1)) rectangular(keySet.offset(-width, 0, -width), keySet.offset(width, height, length), blockstate, blockMap, 0);
            if (value.get(2)) rectangular(keySet.offset(width, 0, width), keySet.offset(-length, height, -width), blockstate, blockMap, 0);
            if (value.get(3)) rectangular(keySet.offset(width, 0, width), keySet.offset(-width, height, -length), blockstate, blockMap, 0);
        }
    }

    //列表快捷填充
    //在整个坐标列表上填充球体，带有半径渐变
    public static void lineSet(List<Vector3d> VctList, double rStart, double rEnd, int blockstate, boolean replace, Object2IntMap<BlockPos> blockMap) {
        double step = (rEnd - rStart) / VctList.size();
        int i = 0;
        for (Vector3d posPoint : VctList) {
            ball(rStart + step * i++, VectorUtils.fromVector3d(posPoint), blockstate, replace, blockMap);
        }
    }

    //在整个坐标列表上填充球体，带有指定y坐标上下不同种方块填充
    public static void lineSet(List<Vector3d> VctList, double rStart, double rEnd, int blockstate1, int blockstate2, boolean replace, Object2IntMap<BlockPos> blockMap, int checkY) {
        double step = (rEnd - rStart) / VctList.size();
        int i = 0;
        for (Vector3d posPoint : VctList) {
            ball(rStart + step * i++, VectorUtils.fromVector3d(posPoint), blockstate1, blockstate2, replace, blockMap, checkY);
        }
    }

    //在整个坐标列表上填充椭球体
    public static void lineSetEllipsoid(List<Vector3d> VctList, double radiusDX, double radiusDY, double radiusDZ, int blockstate, boolean replace, Object2IntMap<BlockPos> blockMap) {
        for (Vector3d posPoint : VctList) {
            ellipsoid(radiusDX, radiusDY, radiusDZ, VectorUtils.fromVector3d(posPoint), blockstate, replace, blockMap);
        }
    }

    //在整个坐标列表上填充球体，带有半径渐变、随机比例
    public static void lineSet(List<Vector3d> VctList, double rStart, double rEnd, int blockstate, boolean replace, Object2IntMap<BlockPos> blockMap, float placePer, WorldgenRandom random) {
        double step = (rEnd - rStart) / VctList.size();
        int i = 0;
        for (Vector3d posPoint : VctList) {
            ball(rStart + step * i++, VectorUtils.fromVector3d(posPoint), blockstate, replace, blockMap, placePer, random);
        }
    }

    //在整个坐标列表上填充椭球体，带有随机比例
    public static void lineSetEllipsoid(List<Vector3d> VctList, double radiusDX, double radiusDY, double radiusDZ, int blockstate, boolean replace, Object2IntMap<BlockPos> blockMap, float placePer, WorldgenRandom random) {
        for (Vector3d posPoint : VctList) {
            ellipsoid(radiusDX, radiusDY, radiusDZ, VectorUtils.fromVector3d(posPoint), blockstate, replace, blockMap, placePer, random);
        }
    }

    //在整个坐标列表上放置地物
    public static void lineSetFeature(List<Vector3d> list, Map<BlockPos, ResourceLocation> featureMap, ResourceLocation[] feature, WorldgenRandom random) {
        int length = feature.length;
        for (Vector3d vctPos : list) {
            featureMap.put(VectorUtils.fromVector3d(vctPos), feature[random.nextInt(length)]);
        }
    }

    //快捷方法整合
    //不规则球体填充，带有壁厚、随机比例
    public static void ball(int radius, int wall, BlockPos centerPos, Object2IntMap<BlockPos> blockMap, float chance, WorldgenRandom random, int wallBlock, int airBlock) {
        List<Vector3d> list = ballPos(radius, centerPos, chance, random);
        lineSet(list, radius, radius, wallBlock, true, blockMap);
        lineSet(list, radius - wall, radius - wall, airBlock, true, blockMap);
    }

    //不规则球体填充，带有壁厚、随机比例、指定y坐标上下不同种方块填充
    public static void ball(int radius, int wall, BlockPos centerPos, Object2IntMap<BlockPos> blockMap, float chance, WorldgenRandom random, int wallBlock, int airBlock1, int airBlock2, int checkY) {
        List<Vector3d> list = ballPos(radius, centerPos, chance, random);
        lineSet(list, radius, radius, wallBlock, true, blockMap);
        lineSet(list, radius - wall, radius - wall, airBlock1, airBlock2, true, blockMap, checkY);
    }

    //获取xz在高度图上的y坐标
    public static int getHeight(int x, int z, Structure.GenerationContext context) {
        return context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
    }
}
