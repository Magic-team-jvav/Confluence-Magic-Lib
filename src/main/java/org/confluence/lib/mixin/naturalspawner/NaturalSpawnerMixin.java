package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.LevelChunk;
import org.confluence.lib.mixed.ILibChunkSpawnDataAccess;
import org.confluence.lib.util.NaturalSpawnerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @ModifyVariable(method = "spawnForChunk", at = @At("STORE"), name = "mobcategory")
    private static MobCategory set(
            MobCategory mobcategory,
            @Local(argsOnly = true) ServerLevel level,
            @Local(argsOnly = true) LevelChunk chunk
    ) {
        ILibChunkSpawnDataAccess.of(mobcategory).confluence$setData(NaturalSpawnerUtils.getChunkSpawnData(level.dimension(), chunk.getPos()));
        return mobcategory;
    }

    @WrapOperation(method = "spawnForChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/NaturalSpawner;spawnCategoryForChunk(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/LevelChunk;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V"))
    private static void reset(
            MobCategory category,
            ServerLevel level,
            LevelChunk chunk,
            NaturalSpawner.SpawnPredicate filter,
            NaturalSpawner.AfterSpawnCallback callback,
            Operation<Void> original
    ) {
        original.call(category, level, chunk, filter, callback);
        ILibChunkSpawnDataAccess.of(category).confluence$setData(NaturalSpawnerUtils.ChunkSpawnData.DEFAULT);
    }

    @Definition(id = "k", local = @Local(type = int.class, ordinal = 2))
    @Expression("k < 3")
    @ModifyExpressionValue(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean modify(
            boolean original,
            @Local(argsOnly = true) MobCategory category,
            @Local(name = "k") int k,
            @Share("frequency") LocalIntRef frequency,
            @Share("obtained") LocalBooleanRef obtained
    ) {
        if (category.isPersistent()) {
            return original;
        }
        return NaturalSpawnerUtils.modifySpeed(original, category, k, frequency, obtained);
    }
}
