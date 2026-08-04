package org.confluence.lib.common.block;

import net.minecraft.world.level.block.state.BlockState;

public interface ILibSimulatorBlock {
    BlockState getSimulatedBlock(boolean isClient);
}
