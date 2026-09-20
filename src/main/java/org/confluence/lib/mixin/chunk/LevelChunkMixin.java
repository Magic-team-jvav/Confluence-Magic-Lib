package org.confluence.lib.mixin.chunk;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.*;
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
import org.confluence.lib.mixed.IPalettedContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
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

    @Inject(method = "setBlockState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getBlock()Lnet/minecraft/world/level/block/Block;"/*这个位置才开始真正的放方块流程*/))
    private void setBlock(CallbackInfoReturnable<BlockState> cir, @Local(argsOnly = true) BlockPos pos, @Local(argsOnly = true) BlockState targetState, @Local LevelChunkSection section, @Local(ordinal = 1) BlockState beforeState) {
        if (!DynamicBiomeUtils.isEnabled()) return; // 未消费 DynamicBiomeUtils.enable() 时零开销
        // 只维护该方块命中的计数器（按状态缓存），不再遍历整张注册表
        BlockCounters.applyChange(beforeState, targetState, ILevelChunkSection.of(section).confluence$getBlockCounts());

        HolderLookup.RegistryLookup<Biome> lookup = level.registryAccess().lookupOrThrow(Registries.BIOME);
        Holder<Biome> resultBiome = DynamicBiomeUtils.judgeSection(section, lookup);
        LevelChunkSection aboveSection = confluence$getAboveSection(pos);
        Holder<Biome> aboveResult = confluence$checkAbove(pos, lookup);
        if (resultBiome != null) {
            confluence$infect(section, resultBiome);
            if (aboveSection == null) return;
            if (aboveResult == null || DynamicBiomeUtils.priorityOf(resultBiome) < DynamicBiomeUtils.priorityOf(aboveResult)) {
                confluence$infect(aboveSection, resultBiome);
            }
        } else {
            if (aboveSection != null && aboveResult == null) {
                confluence$purify(aboveSection);
            }
            Holder<Biome> belowResult = confluence$checkBelow(pos, lookup);
            if (belowResult != null) {
                confluence$infect(section, belowResult);
            } else {
                confluence$purify(section);
            }
        }
    }

    @Unique
    @Nullable
    private LevelChunkSection confluence$getAboveSection(BlockPos pPos) {
        BlockPos belowPos = pPos.offset(0, 16, 0);
        if (level.isOutsideBuildHeight(belowPos.getY())) return null;
        return getSection(getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(belowPos.getY())));
    }

    @Unique
    @Nullable
    private Holder<Biome> confluence$checkAbove(BlockPos pPos, HolderLookup.RegistryLookup<Biome> lookup) {
        LevelChunkSection aboveSection = confluence$getAboveSection(pPos);
        if (aboveSection == null) return null;
        return DynamicBiomeUtils.judgeSection(aboveSection, lookup);
    }

    @Unique
    @Nullable
    private Holder<Biome> confluence$checkBelow(BlockPos pPos, HolderLookup.RegistryLookup<Biome> lookup) {
        BlockPos belowPos = pPos.offset(0, -16, 0);
        if (level.isOutsideBuildHeight(belowPos.getY())) return null;
        LevelChunkSection belowSection = getSection(getSectionIndexFromSectionY(SectionPos.blockToSectionCoord(belowPos.getY())));
        return DynamicBiomeUtils.judgeSection(belowSection, lookup);
    }

    @Unique
    private void confluence$purify(LevelChunkSection section) {
        if (section.getBiomes().maybeHas(DynamicBiomeUtils::isSpreadable)) {
            ILevelChunkSection iSection = ILevelChunkSection.of(section);
            iSection.confluence$setBiomes(iSection.confluence$getBackupBiome());
        }
    }

    @Unique
    private void confluence$infect(LevelChunkSection section, @NotNull Holder<Biome> biome) {
        if (((PalettedContainer<Holder<Biome>>) section.getBiomes()).data.palette().getSize() == 1 && section.getBiomes().maybeHas(b -> b == biome)) {
            return;
        }
        ILevelChunkSection.of(section).confluence$setBiomes(IPalettedContainer.recreateSingle(section.getBiomes(), biome));
    }
}
