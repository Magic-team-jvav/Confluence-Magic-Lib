package org.confluence.lib.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.StemGrownBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.mesdag.portlib.diff.Diff;

import java.util.function.Supplier;

@Diff
public class SupAttachedStemBlock extends AttachedStemBlock {
    protected final Supplier<? extends StemGrownBlock> fruitSup;

    public SupAttachedStemBlock(Supplier<? extends StemGrownBlock> fruitSup, Supplier<Item> seedSupplier, Properties properties) {
        super((StemGrownBlock) Blocks.PUMPKIN, seedSupplier, properties);
        this.fruitSup = fruitSup;
    }

    @Override
    public BlockState updateShape(BlockState state, Direction facing, BlockState facingState, LevelAccessor level, BlockPos currentPos, BlockPos facingPos) {
        return !facingState.is(this.fruitSup.get()) && facing == state.getValue(FACING)
                ? this.fruitSup.get().getStem().defaultBlockState().setValue(StemBlock.AGE, 7)
                : super.updateShape(state, facing, facingState, level, currentPos, facingPos);
    }
}
