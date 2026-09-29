package org.confluence.lib.mixin.chunk;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.worldgen.biome.BlockCounters;
import org.confluence.lib.common.worldgen.biome.BlockCounts;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.confluence.lib.mixed.IPalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelChunkSection.class)
public abstract class LevelChunkSectionMixin implements ILevelChunkSection {
    @Shadow
    private PalettedContainerRO<Holder<Biome>> biomes;

    @Shadow
    public abstract void recalcBlockCounts();

    @Unique
    private PalettedContainerRO<Holder<Biome>> confluence$backupBiome;
    /// 懒分配：`BlockCounters.enable()` 之前不占用内存。
    ///
    /// 这里**不能**用一个共享的空 `BlockCounts` 兜底：计数是可变的，而写入方
    /// （`BlockCounterMixin` / `LevelChunkMixin`）拿到的就是这个实例，共享会让所有区块
    /// 往同一个对象里累加。所以缺省就按需分配。
    @Unique
    private BlockCounts confluence$blockCounts;
    /// 见 {@link ILevelChunkSection#confluence$isCountsFresh()}
    @Unique
    private boolean confluence$countsFresh;

    @Override
    public boolean confluence$isCountsFresh() {
        return confluence$countsFresh;
    }

    @Override
    public void confluence$setCountsFresh(boolean fresh) {
        this.confluence$countsFresh = fresh;
    }

    @Override
    public BlockCounts confluence$getBlockCounts() {
        BlockCounts counts = confluence$blockCounts;
        if (counts == null) {
            confluence$blockCounts = counts = new BlockCounts();
        }
        return counts;
    }

    @Override
    public PalettedContainerRO<Holder<Biome>> confluence$getBackupBiome() {
        return confluence$backupBiome;
    }

    @Override
    public void confluence$setBackupBiome(PalettedContainerRO<Holder<Biome>> biome) {
        this.confluence$backupBiome = biome;
    }

    @Override
    public void confluence$setBiomes(PalettedContainerRO<Holder<Biome>> biomes) {
        this.biomes = biomes;
    }

    /// 不写这个会没有初始化confluence$backupBiome，导致区块保存失败
    /// [ChunkSerializerMixin#write]
    ///
    /// 新建 section（世界生成路径）：vanilla 构造器**没有**重算方块数，所以标为不新鲜，
    /// 由 `DynamicBiomeUtils.applyDynamicBiome` 在区块成型时补一次完整重算。
    @Inject(method = "<init>(Lnet/minecraft/core/Registry;)V", at = @At("TAIL"))
    private void constrNew(CallbackInfo ci) {
        this.confluence$backupBiome = IPalettedContainer.copyBiomes(biomes);
        // 启用后即时分配：避免区块生成时首次访问的写入竞争。
        // 注意别覆盖构造过程中（recalcBlockCounts 里）已经懒分配出来的实例，否则会丢掉刚算好的计数。
        if (BlockCounters.isEnabled() && this.confluence$blockCounts == null) {
            this.confluence$blockCounts = new BlockCounts();
        }
        this.confluence$countsFresh = false;
    }

    /// 读档路径：vanilla 构造器末尾自己会 `recalcBlockCounts()`，所以构造完成后计数就是新鲜的，
    /// 不需要 `applyDynamicBiome` 再算一遍。
    @Inject(method = "<init>(Lnet/minecraft/world/level/chunk/PalettedContainer;Lnet/minecraft/world/level/chunk/PalettedContainerRO;)V", at = @At("TAIL"))
    private void constrLoaded(CallbackInfo ci) {
        this.confluence$backupBiome = IPalettedContainer.copyBiomes(biomes);
        if (BlockCounters.isEnabled() && this.confluence$blockCounts == null) {
            this.confluence$blockCounts = new BlockCounts();
        }
        this.confluence$countsFresh = BlockCounters.isEnabled();
    }

    /// 完整重算前先清零。
    ///
    /// 计数器是「累加」语义（见 `BlockCounterMixin`），而 `recalcBlockCounts()` 可能被跑多次
    /// （构造器里一次、`applyDynamicBiome` 补一次、`read` 之后一次）。
    /// 不清零就会把计数翻倍，阈值判定跟着失真。
    @Inject(method = "recalcBlockCounts", at = @At("HEAD"))
    private void clearCounts(CallbackInfo ci) {
        if (!BlockCounters.isEnabled()) return;
        confluence$getBlockCounts().clear();
    }

    /// 平坦世界等生成器同样经过这里，不能只在噪声生成器里保留底图。
    @Inject(method = "fillBiomesFromNoise", at = @At("RETURN"))
    private void captureGeneratedBiomes(CallbackInfo ci) {
        this.confluence$backupBiome = IPalettedContainer.copyBiomes(biomes);
    }

    /// 客户端从网络读到 section 后补一次重算（vanilla 的 read 不算方块数），
    /// 否则客户端侧拿不到计数（`EctoMistHelper` 之类要用）。
    @Inject(method = "read", at = @At("RETURN"))
    private void read(CallbackInfo ci) {
        if (!BlockCounters.isEnabled()) return;
        try {
            recalcBlockCounts();
            this.confluence$countsFresh = true;
        } catch (Exception e) {
            ConfluenceMagicLib.LOGGER.warn("Failed to recalc block counts");
        }
    }
}
