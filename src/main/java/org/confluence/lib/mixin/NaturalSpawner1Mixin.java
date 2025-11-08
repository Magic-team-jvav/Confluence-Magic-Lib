package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.confluence.lib.ConfluenceMagicLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawner1Mixin {
    @Shadow
    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double distance) {
        return false;
    }

    @Definition(id = "k", local = @Local(type = int.class, ordinal = 2))
    @Expression("k < 3")
    @ModifyExpressionValue(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At("MIXINEXTRAS:EXPRESSION"))
    private static boolean confluenceLib$wrap(boolean original,
                                              @Local(argsOnly = true) ServerLevel level,
                                              @Local(argsOnly = true) BlockPos pos,
                                              @Local(argsOnly = true) ChunkAccess chunk,
                                              @Local(ordinal = 2) int k,
                                              @Local(ordinal = 0) BlockPos.MutableBlockPos blockpos$mutableblockpos,
                                              @Share("frequency") LocalIntRef frequency,
                                              @Share("isObtain") LocalBooleanRef isObtain) {
        if (isObtain.get()) {
            return frequency != null ? k < frequency.get() : original;
        }
        var x = pos.getX();
        var y = pos.getY();
        var z = pos.getZ();
        Player player = level.getNearestPlayer(x, y, z, -1.0, false);
        if (player == null || !player.getAttributes().hasAttribute(ConfluenceMagicLib.ENEMY_SPAWN_SPEED_MULTIPLIER) ||
                !isRightDistanceToPlayerAndSpawnPoint(level, chunk, blockpos$mutableblockpos, player.distanceToSqr(x, y, z))) {
            return original;
        }
        frequency.set(Mth.ceil(3 * player.getAttributeValue(ConfluenceMagicLib.ENEMY_SPAWN_SPEED_MULTIPLIER)));
        isObtain.set(true);
        return k < frequency.get();
    }

//    @ModifyConstant(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;" +
//            "Lnet/minecraft/server/level/ServerLevel;" +
//            "Lnet/minecraft/world/level/chunk/ChunkAccess;" +
//            "Lnet/minecraft/core/BlockPos;" +
//            "Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;" +
//            "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
//            constant = @Constant(intValue = 3))
//    private static int confluenceLib$modifyOuterLoopCount(int original,
//                                                          @Local(argsOnly = true) ServerLevel level,
//                                                          @Local(argsOnly = true) BlockPos pos,
//                                                          @Local(argsOnly = true) ChunkAccess chunk,
//                                                          @Local BlockPos.MutableBlockPos blockpos$mutableblockpos,
//                                                          @Local BlockState blockstate) {
//
//    }
//
//    @ModifyVariable(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I"), ordinal = 2)
//    private static int modifyK(int value) {
//
//    }

//    @ModifyVariable(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I"), ordinal = 2)
//    private static int modifyK(int value) {
//
//    }

//    @ModifyVariable(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/chunk/ChunkAccess;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;getX()I"), ordinal = 2)
//    private static int confluenceLib$modifyK(int value,
//                                             @Local(argsOnly = true) ServerLevel level,
//                                             @Local(argsOnly = true) BlockPos pos,
//                                             @Local(argsOnly = true) ChunkAccess chunk,
//                                             @Local(ordinal = 2) int k,
//                                             @Local BlockPos.MutableBlockPos blockpos$mutableblockpos,
//                                             @Share("frequency") LocalIntRef frequency,
//                                             @Share("isObtain") LocalBooleanRef isObtain) {
//        if (isObtain.get()) {
//            return value;
//        }
//        var x = pos.getX();
//        var y = pos.getY();
//        var z = pos.getZ();
//        Player player = level.getNearestPlayer(x, y, z, -1.0, true);
//        if (player == null || !player.getAttributes().hasAttribute(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER) ||
//                !isRightDistanceToPlayerAndSpawnPoint(level, chunk, blockpos$mutableblockpos, player.distanceToSqr(x, y, z))) {
//            return value;
//        }
//        frequency.set((int) (3 * player.getAttributeValue(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER)));
//        isObtain.set(true);
//        return value;
//    }
}
