package org.confluence.lib.mixin.client;

import net.minecraft.client.renderer.block.BlockModelSet;
import net.minecraft.world.level.block.state.BlockState;
import org.confluence.lib.common.block.ILibSimulatorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = BlockModelSet.class, priority = 900)
public abstract class BlockModelSetMixin {
    @ModifyVariable(method = "get", at = @At("HEAD"), argsOnly = true, name = "blockState")
    private BlockState simulator(BlockState state) {
        if (state.getBlock() instanceof ILibSimulatorBlock simulatorBlock) {
            state = simulatorBlock.getSimulatedBlock(true);
        }
        return state;
    }
}
