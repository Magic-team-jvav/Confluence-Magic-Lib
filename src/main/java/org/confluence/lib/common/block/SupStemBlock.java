package org.confluence.lib.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.portlib.diff.Diff;

import java.util.function.Supplier;

@Diff
public class SupStemBlock extends StemBlock {
    protected final Supplier<? extends StemGrownBlock> fruitSup;

    public SupStemBlock(Supplier<? extends StemGrownBlock> fruitSup, Supplier<Item> seedSupplier, Properties properties) {
        super((StemGrownBlock) Blocks.PUMPKIN, seedSupplier, properties);
        this.fruitSup = fruitSup;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.isAreaLoaded(pos, 1)) return;
        if (level.getRawBrightness(pos, 0) >= 9) {
            float f = CropBlock.getGrowthSpeed(this, level, pos);
            if (net.minecraftforge.common.ForgeHooks.onCropsGrowPre(level, pos, state, random.nextInt((int) (25.0F / f) + 1) == 0)) {
                int i = state.getValue(AGE);
                if (i < 7) {
                    level.setBlock(pos, state.setValue(AGE, i + 1), 2);
                } else {
                    Direction direction = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                    BlockPos blockpos = pos.relative(direction);
                    BlockState blockstate = level.getBlockState(blockpos.below());
                    if (level.isEmptyBlock(blockpos) && (blockstate.canSustainPlant(level, blockpos.below(), Direction.UP, this.fruitSup.get()) || blockstate.is(Blocks.FARMLAND) || blockstate.is(BlockTags.DIRT))) {
                        level.setBlockAndUpdate(blockpos, getFruit().defaultBlockState());
                        level.setBlockAndUpdate(pos, getFruit().getAttachedStem().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, direction));
                    }
                }
                net.minecraftforge.common.ForgeHooks.onCropsGrowPost(level, pos, state);
            }

        }
    }

    @Override
    public StemGrownBlock getFruit() {
        return fruitSup.get();
    }
}
