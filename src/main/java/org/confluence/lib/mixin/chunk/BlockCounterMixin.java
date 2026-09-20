package org.confluence.lib.mixin.chunk;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.confluence.lib.common.worldgen.biome.BlockCounters;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 从硬盘加载区块的时候会重新数方块
@Mixin(targets = "net.minecraft.world.level.chunk.LevelChunkSection$1BlockCounter")
public abstract class BlockCounterMixin {
    @Unique
    private @Nullable ILevelChunkSection confluence$section;

    @Dynamic // 抑制一下报错
    @Inject(method = "accept", at = @At("RETURN"))
    private void accept(BlockState state, int count, CallbackInfo ci) {
        if (confluence$section == null || !BlockCounters.isEnabled()) return;
        // 只给该方块命中的计数器累加，不再遍历整张注册表
        BlockCounters.add(state, count, confluence$section.confluence$getBlockCounts());
    }

    @Dynamic
    @Inject(method = "<init>*", at = @At("RETURN"))
    private void constr(LevelChunkSection section, CallbackInfo ci) {
        this.confluence$section = ILevelChunkSection.of(section);
    }
}
