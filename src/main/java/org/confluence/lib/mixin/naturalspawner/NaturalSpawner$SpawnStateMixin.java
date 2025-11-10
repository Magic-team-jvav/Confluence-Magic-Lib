package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.confluence.lib.mixed.NaturalSpawnerData;
import org.confluence.lib.util.NaturalSpawnerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class NaturalSpawner$SpawnStateMixin {
    @Definition(id = "category", local = @Local(type = MobCategory.class, argsOnly = true))
    @Definition(id = "getMaxInstancesPerChunk", method = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I")
    @Definition(id = "spawnableChunkCount", field = "Lnet/minecraft/world/level/NaturalSpawner$SpawnState;spawnableChunkCount:I")
    @Definition(id = "MAGIC_NUMBER", field = "Lnet/minecraft/world/level/NaturalSpawner;MAGIC_NUMBER:I")
    @Expression("category.getMaxInstancesPerChunk() * this.spawnableChunkCount / MAGIC_NUMBER")
    @ModifyExpressionValue(method = "canSpawnForCategory", at = @At("MIXINEXTRAS:EXPRESSION"))
    private int confluenceLib$canSpawnForCategory(int o,
                                                  @Local(argsOnly = true) MobCategory category,
                                                  @Local(argsOnly = true) ChunkPos pos) {
        return NaturalSpawnerUtil.confluenceLib$canSpawnForCategory(o, category, pos, NaturalSpawnerData.spawnForChunkServerLevel.get());
    }
}
