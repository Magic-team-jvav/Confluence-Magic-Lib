package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.confluence.lib.ConfluenceMagicLib;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {

    @Shadow
    private static boolean isRightDistanceToPlayerAndSpawnPoint(ServerLevel level, ChunkAccess chunk, BlockPos.MutableBlockPos pos, double distance) {
        return false;
    }

    @ModifyConstant(method = "spawnCategoryForPosition(Lnet/minecraft/world/entity/MobCategory;" +
            "Lnet/minecraft/server/level/ServerLevel;" +
            "Lnet/minecraft/world/level/chunk/ChunkAccess;" +
            "Lnet/minecraft/core/BlockPos;" +
            "Lnet/minecraft/world/level/NaturalSpawner$SpawnPredicate;" +
            "Lnet/minecraft/world/level/NaturalSpawner$AfterSpawnCallback;)V",
            constant = @Constant(intValue = 3))
    private static int confluenceLib$modifyOuterLoopCount(int original,
                                                          @Local(argsOnly = true) ServerLevel level,
                                                          @Local(argsOnly = true) BlockPos pos,
                                                          @Local(argsOnly = true) ChunkAccess chunk,
                                                          @Local BlockPos.MutableBlockPos blockpos$mutableblockpos,
                                                          @Local BlockState blockstate) {
        var x = pos.getX();
        var y = pos.getY();
        var z = pos.getZ();
        Player player = level.getNearestPlayer(x, y, z, -1.0, true);
        if (player == null ||
                !player.getAttributes().hasAttribute(ConfluenceMagicLib.PLAYER_MONSTER_SPAWN_COUNT_FACTOR) ||
                !isRightDistanceToPlayerAndSpawnPoint(level, chunk, blockpos$mutableblockpos, player.distanceToSqr(x, y, z))) {
            return original;
        }
        return (int) (original * player.getAttributeValue(ConfluenceMagicLib.PLAYER_MONSTER_SPAWN_COUNT_FACTOR));
    }
}
