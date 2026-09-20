package org.confluence.lib.mixin.chunk;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import org.confluence.lib.common.worldgen.biome.DynamicBiomeUtils;
import org.confluence.lib.mixed.ILevelChunkSection;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// 把动态群系用到的「纯净群系备份」存进区块 NBT（字段名 `backup_biome`）。
///
/// 读不到该字段时按当前群系重新推导，所以**旧存档可直接兼容**：
/// 老区块没有这个字段，首次读取会就地推导一份。
@Mixin(ChunkSerializer.class)
public abstract class ChunkSerializerMixin {
    @Shadow
    private static void logErrors(ChunkPos chunkPos, int chunkSectionY, String errorMessage) {}

    @Shadow
    @Final
    private static Logger LOGGER;

    @Inject(method = "write", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunkSection;getBiomes()Lnet/minecraft/world/level/chunk/PalettedContainerRO;"))
    private static void write(
            CallbackInfoReturnable<CompoundTag> cir,
            @Local(name = "codec") Codec<PalettedContainerRO<Holder<Biome>>> codec,
            @Local(name = "compoundtag1") CompoundTag compoundtag1,
            @Local(name = "levelchunksection") LevelChunkSection levelchunksection
    ) {
        compoundtag1.put("backup_biome", codec.encodeStart(NbtOps.INSTANCE, ILevelChunkSection.of(levelchunksection).confluence$getBackupBiome()).getOrThrow(false, LOGGER::warn));
    }

    @Inject(method = "read", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/ai/village/poi/PoiManager;checkConsistencyWithBlocks(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/chunk/LevelChunkSection;)V"))
    private static void read(
            CallbackInfoReturnable<ProtoChunk> cir,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) ChunkPos pos,
            @Local(name = "codec") Codec<PalettedContainerRO<Holder<Biome>>> codec,
            @Local(name = "compoundtag") CompoundTag compoundtag,
            @Local(name = "k") int k,
            @Local(name = "levelchunksection") LevelChunkSection levelchunksection
    ) {
        // 从原来的方法里面抄的
        PalettedContainerRO<Holder<Biome>> bakBiome;
        if (compoundtag.contains("backup_biome", Tag.TAG_COMPOUND)) {
            bakBiome = codec.parse(NbtOps.INSTANCE, compoundtag.getCompound("backup_biome")).promotePartial(err -> logErrors(pos, k, err)).getOrThrow(false, LOGGER::warn);
        } else {
            bakBiome = DynamicBiomeUtils.judgeBackupBiome(levelchunksection, level.registryAccess().lookupOrThrow(Registries.BIOME));
        }
        ILevelChunkSection.of(levelchunksection).confluence$setBackupBiome(bakBiome);
    }
}
