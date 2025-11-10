package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.NaturalSpawner;
import org.confluence.lib.mixed.NaturalSpawnerData;
import org.confluence.lib.util.NaturalSpawnerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class NaturalSpawner$SpawnStateMixin {
    @ModifyVariable(method = "canSpawnForCategory", at = @At("STORE"))
    private int confluenceLib$canSpawnForCategory(int original, @Local(argsOnly = true) MobCategory category, @Local(argsOnly = true) ChunkPos pos) {
        return NaturalSpawnerUtil.confluenceLib$canSpawnForCategory(original, category, pos, NaturalSpawnerData.spawnForChunkServerLevel.get());
    }
}
