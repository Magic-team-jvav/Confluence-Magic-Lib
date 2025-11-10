package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.confluence.lib.mixed.NaturalSpawnerData;
import org.confluence.lib.util.NaturalSpawnerUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalMobCapCalculator.MobCounts.class)
public abstract class LocalMobCapCalculator$MobCountsMixin {
    @ModifyExpressionValue(method = "canSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/MobCategory;getMaxInstancesPerChunk()I"))
    public int confluenceLib$canSpawn(int original, @Local(argsOnly = true) MobCategory category) {
        return NaturalSpawnerUtil.confluenceLib$canSpawn(original, category, NaturalSpawnerData.canSpawnServerPlayer.get());
    }
}
