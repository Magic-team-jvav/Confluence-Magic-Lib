package org.confluence.lib.util;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public final class FeatureUtils {
    public static boolean safeSetBlock(WorldGenLevel level, BlockPos pos, BlockState state, Predicate<BlockState> oldState) {
        if (oldState.test(level.getBlockState(pos))) {
            return level.setBlock(pos, state, 3);
        }
        return false;
    }

    public static boolean isPosAir(WorldGenLevel level, BlockPos blockPos) {
        return level.isStateAtPosition(blockPos, BlockBehaviour.BlockStateBase::isAir);
    }

    public static boolean isPosLiquid(WorldGenLevel level, BlockPos blockPos) {
        return level.isStateAtPosition(blockPos, BlockBehaviour.BlockStateBase::liquid);
    }

    public static boolean isPosSturdy(WorldGenLevel level, BlockPos blockPos, Direction face) {
        return level.isStateAtPosition(blockPos, blockState -> blockState.isFaceSturdy(level, blockPos, face));
    }

    public static void leaves(BoundingBox box, BlockState leaves, boolean up, RandomSource random, WorldGenLevel level, BlockState droopingLeaves, boolean droop) {
        int xStart = box.minX();
        int yStart = box.minY();
        int zStart = box.minZ();
        int xEnd = box.maxX();
        int yEnd = box.maxY();
        int zEnd = box.maxZ();
        boolean set;
        BlockPos posPlace;
        BlockPos posDroop;
        int yDroop;
        int length;
        for (int x = xStart; x <= xEnd; x++) {
            for (int y = yStart; y <= yEnd; y++) {
                for (int z = zStart; z <= zEnd; z++) {
                    posPlace = new BlockPos(x, y, z);
                    set = (!((x == xStart || x == xEnd) && (z == zStart || z == zEnd)) || ((y == yStart || up) && random.nextInt(3) == 0)) && (level.getBlockState(posPlace).isAir());
                    if (set) {
                        level.setBlock(posPlace, leaves, 3);
                    }
                    if (droop) {
                        if (posPlace.getY() == yStart) {
                            yDroop = posPlace.getY() - 1;
                            length = (level.getBlockState(posPlace).isAir()) ? 0 : random.nextInt(4);
                            for (int i = 0; i < length; i++) {
                                posDroop = new BlockPos(x, yDroop - i, z);
                                if (level.getBlockState(posDroop).isAir()) {
                                    level.setBlock(posDroop, droopingLeaves, 3);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static @Nullable BlockEntity getBlockEntity(WorldGenLevel level, BlockPos blockPos) {
        BlockEntity blockEntity = level.getBlockEntity(blockPos);
        if (blockEntity == null) {
            LibUtils.devRun(() -> ConfluenceMagicLib.LOGGER.warn("Failed to fetch block entity at ({}, {}, {})", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            return null;
        }
        return blockEntity;
    }

    public static void ball8(BlockPos.MutableBlockPos posCheck, boolean replace, int x, int y, int z, BlockState blockState, BlockPos centerPos, WorldGenLevel level) {
        for (int i = 0; i < 8; i++) {
            posCheck.set(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
            if (replace || level.getBlockState(posCheck).isAir()) {
                level.setBlock(posCheck, blockState, 3);
            }
        }
    }

    public static void ball8(BlockPos.MutableBlockPos posCheck, boolean replace, int x, int y, int z, Function<BlockState, @Nullable BlockState> blockState, BlockPos centerPos, WorldGenLevel level) {
        for (int i = 0; i < 8; i++) {
            posCheck.set(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
            BlockState state = level.getBlockState(posCheck);
            if (replace || state.isAir()) {
                BlockState apply = blockState.apply(state);
                if (apply != null) {
                    level.setBlock(posCheck, apply, 3);
                }
            }
        }
    }

    public static void ball8(BlockPos.MutableBlockPos posCheck, boolean replace, int x, int y, int z, BlockState blockState1, BlockState blockState2, BlockPos centerPos, WorldGenLevel level, int checkY) {
        for (int i = 0; i < 8; i++) {
            posCheck.set(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
            if (replace || level.getBlockState(posCheck).isAir()) {
                if (posCheck.getY() > checkY) {
                    level.setBlock(posCheck, blockState1, 3);
                } else {
                    level.setBlock(posCheck, blockState2, 3);
                }
            }
        }
    }

    public static void ball8(BlockPos.MutableBlockPos posCheck, boolean replace, int x, int y, int z, BlockState blockState, BlockPos centerPos, WorldGenLevel level, float placePer, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            if (placePer >= random.nextFloat()) {
                posCheck.set(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
                if (replace || level.getBlockState(posCheck).isAir()) {
                    level.setBlock(posCheck, blockState, 3);
                }
            }
        }
    }

    public static void ball8(BlockPos.MutableBlockPos posCheck, boolean replace, int x, int y, int z, Function<BlockState, @Nullable BlockState> blockState, BlockPos centerPos, WorldGenLevel level, float placePer, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            if (placePer >= random.nextFloat()) {
                posCheck.set(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
                BlockState state = level.getBlockState(posCheck);
                if (replace || state.isAir()) {
                    BlockState apply = blockState.apply(state);
                    if (apply != null) {
                        level.setBlock(posCheck, apply, 3);
                    }
                }
            }
        }
    }

    // 填充方法
    // 球体填充
    public static void ball(double radiusD, BlockPos centerPos, BlockState blockState, boolean replace, WorldGenLevel level) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(posCheck, replace, x, y, z, blockState, centerPos, level);
                    }
                }
            }
        }
    }

    // 球体填充，且允许根据原方块状态灵活调整新方块状态
    public static void ball(double radiusD, BlockPos centerPos, Function<BlockState, @Nullable BlockState> blockState, boolean replace, WorldGenLevel level) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(posCheck, replace, x, y, z, blockState, centerPos, level);
                    }
                }
            }
        }
    }

    // 球体填充，带有指定y坐标上下不同种方块填充
    public static void ball(double radiusD, BlockPos centerPos, BlockState blockState1, BlockState blockState2, boolean replace, WorldGenLevel level, int checkY) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(posCheck, replace, x, y, z, blockState1, blockState2, centerPos, level, checkY);
                    }
                }
            }
        }
    }

    // 球体填充，带有随机比例
    public static void ball(double radiusD, BlockPos centerPos, BlockState blockState, boolean replace, WorldGenLevel level, float placePer, RandomSource random) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(posCheck, replace, x, y, z, blockState, centerPos, level, placePer, random);
                    }
                }
            }
        }
    }

    // 球体填充，带有随机比例，且允许根据原方块状态灵活调整新方块状态
    public static void ball(double radiusD, BlockPos centerPos, Function<BlockState, @Nullable BlockState> blockState, boolean replace, WorldGenLevel level, float placePer, RandomSource random) {
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if ((x2 + y2 + z * z <= radius2)) {
                        ball8(posCheck, replace, x, y, z, blockState, centerPos, level, placePer, random);
                    }
                }
            }
        }
    }

    // 立方体填充
    public static void rectangular(BlockPos startPos, BlockPos endPos, BlockState blockstate, WorldGenLevel level, boolean replace) {
        int startX = Math.min(endPos.getX(), startPos.getX());
        int startY = Math.min(endPos.getY(), startPos.getY());
        int startZ = Math.min(endPos.getZ(), startPos.getZ());
        int endX = Math.max(endPos.getX(), startPos.getX());
        int endY = Math.max(endPos.getY(), startPos.getY());
        int endZ = Math.max(endPos.getZ(), startPos.getZ());
        int xLength = endX - startX;
        int yLength = endY - startY;
        int zLength = endZ - startZ;
        BlockPos.MutableBlockPos posCheck = startPos.mutable();
        for (int x = 0; x <= xLength; x++) {
            for (int y = 0; y <= yLength; y++) {
                for (int z = 0; z <= zLength; z++) {
                    posCheck.set(startX + x, startY + y, startZ + z);
                    if (replace || level.getBlockState(posCheck).canBeReplaced()) level.setBlock(posCheck, blockstate, 3);
                }
            }
        }
    }

    // 椭球体填充
    public static void ellipsoid(double radiusDX, double radiusDY, double radiusDZ, BlockPos centerPos, BlockState blockState, boolean replace, WorldGenLevel level) {
        int radiusX = (int) radiusDX + 1;
        int radiusY = (int) radiusDY + 1;
        int radiusZ = (int) radiusDZ + 1;
        double rX = radiusDX * radiusDX;
        double rY = radiusDY * radiusDY;
        double rZ = radiusDZ * radiusDZ;
        int x2;
        int y2;
        BlockPos.MutableBlockPos posCheck = centerPos.mutable();
        for (int x = 0; x < radiusX; x++) {
            x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if ((x2 / rX + y2 / rY + (z * z) / rZ) <= 1) {
                        ball8(posCheck, replace, x, y, z, blockState, centerPos, level);
                    }
                }
            }
        }
    }

    // 立方体检查
    public static boolean rectangularCheck(BlockPos startPos, BlockPos endPos, WorldGenLevel level) {
        int startX = Math.min(endPos.getX(), startPos.getX());
        int startY = Math.min(endPos.getY(), startPos.getY());
        int startZ = Math.min(endPos.getZ(), startPos.getZ());
        int endX = Math.max(endPos.getX(), startPos.getX());
        int endY = Math.max(endPos.getY(), startPos.getY());
        int endZ = Math.max(endPos.getZ(), startPos.getZ());
        int xLength = endX - startX;
        int yLength = endY - startY;
        int zLength = endZ - startZ;
        boolean bl = true;
        BlockPos.MutableBlockPos posCheck = startPos.mutable();
        for (int x = 0; (x <= xLength) && bl; x++) {
            for (int y = 0; (y <= yLength) && bl; y++) {
                for (int z = 0; (z <= zLength) && bl; z++) {
                    posCheck.set(startX + x, startY + y, startZ + z);
                    if (!level.getBlockState(posCheck).canBeReplaced()) bl = false;
                }
            }
        }
        return bl;
    }

    public static boolean ensureCanWrite(WorldGenLevel level, BlockPos pos) {
        if (level instanceof WorldGenRegion region) {
            int i = SectionPos.blockToSectionCoord(pos.getX());
            int j = SectionPos.blockToSectionCoord(pos.getZ());
            ChunkPos chunkpos = region.getCenter();
            int k = Math.abs(chunkpos.x - i);
            int l = Math.abs(chunkpos.z - j);
            if (k <= region.generatingStep.blockStateWriteRadius() && l <= region.generatingStep.blockStateWriteRadius()) {
                if (region.center.isUpgrading()) {
                    LevelHeightAccessor levelheightaccessor = region.center.getHeightAccessorForGeneration();
                    return pos.getY() >= levelheightaccessor.getMinBuildHeight() && pos.getY() < levelheightaccessor.getMaxBuildHeight();
                }
                return true;
            }
            return false;
        }
        return true;
    }

    //生成一個凸多面體的blockPos列表
    public static void updateLeavesOptimized(WorldGenLevel level, LongOpenHashSet trunkSet, LongOpenHashSet leavesSet, boolean cleanUncovered, boolean debugMode) {
        if (leavesSet.isEmpty()) return;

        Long2IntOpenHashMap distanceMap = new Long2IntOpenHashMap(leavesSet.size());
        distanceMap.defaultReturnValue(-1);
        Queue<Long> queue = new ArrayDeque<>();

        for (long logPos : trunkSet) {
            queue.add(logPos);
            distanceMap.put(logPos, 0);
        }

        BlockState[] debugColors = debugMode ? new BlockState[]{
                Blocks.CYAN_CONCRETE.defaultBlockState(), Blocks.GREEN_CONCRETE.defaultBlockState(),
                Blocks.LIME_CONCRETE.defaultBlockState(), Blocks.YELLOW_CONCRETE.defaultBlockState(),
                Blocks.ORANGE_CONCRETE.defaultBlockState(), Blocks.RED_CONCRETE.defaultBlockState()
        } : null;

        while (!queue.isEmpty()) {
            long currentLong = queue.poll();
            int currentDist = distanceMap.get(currentLong);

            if (currentDist >= 6) continue;

            BlockPos currentPos = BlockPos.of(currentLong);
            for (Direction direction : Direction.values()) {
                long neighborLong = currentPos.relative(direction).asLong();

                if (leavesSet.contains(neighborLong) && distanceMap.get(neighborLong) == -1) {
                    int newDist = currentDist + 1;
                    distanceMap.put(neighborLong, newDist);
                    queue.add(neighborLong);

                    BlockPos neighborPos = BlockPos.of(neighborLong);
                    if (debugMode) {
                        level.setBlock(neighborPos, debugColors[newDist - 1], 3);
                    } else {
                        BlockState state = level.getBlockState(neighborPos);
                        if (state.hasProperty(BlockStateProperties.DISTANCE)) {
                            level.setBlock(neighborPos, state.setValue(BlockStateProperties.DISTANCE, newDist), 19);
                        }
                    }
                }
            }
        }

        if (cleanUncovered) {
            for (long leafLong : leavesSet) {
                if (!trunkSet.contains(leafLong) && distanceMap.get(leafLong) == -1) {
                    level.setBlock(BlockPos.of(leafLong), debugMode ? Blocks.WHITE_CONCRETE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 19);
                }
            }
        }
    }
}
