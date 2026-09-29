package org.confluence.lib.mixin.chunk;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import org.confluence.lib.common.worldgen.biome.BlockCounters;
import org.confluence.lib.common.worldgen.biome.DynamicBiomeUtils;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


/// 动态群系在方块变化时的上下传播。
///
/// 只依赖 {@link DynamicBiomeUtils} 注册进来的规则 / 优先级 / 可蔓延判定，
/// 具体是哪些群系由消费方决定。
@Mixin(LevelChunk.class)
public abstract class LevelChunkMixin extends ChunkAccess {
    @Shadow
    @Final
    Level level;

    private LevelChunkMixin(
            ChunkPos chunkPos,
            UpgradeData upgradeData,
            LevelHeightAccessor levelHeightAccessor,
            Registry<Biome> biomeRegistry,
            long inhabitedTime,
            @Nullable LevelChunkSection[] sections,
            @Nullable BlendingData blendingData
    ) {
        super(chunkPos, upgradeData, levelHeightAccessor, biomeRegistry, inhabitedTime, sections, blendingData);
    }

    @Inject(method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ProtoChunk;Lnet/minecraft/world/level/chunk/LevelChunk$PostLoadProcessor;)V", at = @At("RETURN"))
    private void protoToLevel(CallbackInfo ci, @Local(argsOnly = true) ServerLevel level, @Local(argsOnly = true) ProtoChunk chunk) {
        DynamicBiomeUtils.applyDynamicBiome(chunk, level.registryAccess().lookupOrThrow(Registries.BIOME));
    }

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"))
    private void setBlock(CallbackInfoReturnable<BlockState> cir, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState targetState, @Local LevelChunkSection section, @Local(ordinal = 1) BlockState beforeState) {
        if (!DynamicBiomeUtils.isEnabled()) return; // 未消费 DynamicBiomeUtils.enable() 时零开销
        // 只维护该方块命中的计数器（按状态缓存），不再遍历整张注册表
        BlockCounters.applyChange(beforeState, targetState, ILevelChunkSection.of(section).confluence$getBlockCounts());
        ILevelChunkSection.of(section).confluence$getBlockCounts().changeCell(beforeState, targetState,
                pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);

        if (level instanceof ServerLevel serverLevel && DynamicBiomeUtils.affectsDynamicBiome(beforeState, targetState)) {
            DynamicBiomeUtils.markChanged(serverLevel, pos);
        }
    }
}
