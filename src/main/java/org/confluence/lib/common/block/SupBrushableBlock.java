package org.confluence.lib.common.block;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BrushableBlock;
import org.mesdag.portlib.diff.Diff;

import java.util.function.Supplier;

@Diff
public class SupBrushableBlock extends BrushableBlock {
    protected final Supplier<? extends Block> turnsIntoSup;

    public SupBrushableBlock(Supplier<? extends Block> turnsIntoSup, Properties properties, SoundEvent brushSound, SoundEvent brushCompletedSound) {
        super(Blocks.AIR, properties, brushSound, brushCompletedSound);
        this.turnsIntoSup = turnsIntoSup;
    }

    @Override
    public Block getTurnsInto() {
        return turnsIntoSup.get();
    }
}
