package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.confluence.lib.mixed.NaturalSpawnerData;
import org.confluence.lib.util.NaturalSpawnerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawner1Mixin {

    @Definition(id = "k", local = @Local(type = int.class, ordinal = 2))
    @Expression("k < 3")
    @ModifyExpressionValue(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean confluenceLib$wrap(boolean original,
                                              @Local(argsOnly = true) MobCategory category,
                                              @Local(argsOnly = true) ServerLevel level,
                                              @Local(argsOnly = true) BlockPos pos,
                                              @Local(argsOnly = true) ChunkAccess chunk,
                                              @Local(ordinal = 2) int k,
                                              @Local(ordinal = 0) BlockPos.MutableBlockPos blockpos$mutableblockpos,
                                              @Share("frequency") LocalIntRef frequency,
                                              @Share("isObtain") LocalBooleanRef isObtain) {
        return NaturalSpawnerUtil.confluenceLib$wrap(original, category, level, pos, chunk, k, blockpos$mutableblockpos, frequency, isObtain);
    }

    @Inject(method = "spawnForChunk", at = @At("HEAD"))
    private static void confluenceLib$spawnForChunk(final ServerLevel level,
                                                    final LevelChunk chunk,
                                                    final NaturalSpawner.SpawnState spawnState,
                                                    final boolean spawnFriendlies,
                                                    final boolean spawnMonsters,
                                                    final boolean forcedDespawn,
                                                    final CallbackInfo ci) {
        NaturalSpawnerData.spawnForChunkServerLevel.set(level);
    }
}
