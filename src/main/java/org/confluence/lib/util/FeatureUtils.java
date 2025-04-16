package org.confluence.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

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
            LibUtils.devRun(() -> ConfluenceMagicLib.LOGGER.error("Failed to fetch block entity at ({}, {}, {})", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            return null;
        }
        return blockEntity;
    }
}
