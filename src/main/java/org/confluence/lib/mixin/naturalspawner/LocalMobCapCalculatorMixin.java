package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.LocalMobCapCalculator;
import org.confluence.lib.mixed.NaturalSpawnerData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalMobCapCalculator.class)
public abstract class LocalMobCapCalculatorMixin {
    @ModifyExpressionValue(method = "canSpawn", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private <V> V confluenceLib$canSpawn(V original, @Local(argsOnly = true) MobCategory category, @Local(ordinal = 0) ServerPlayer serverplayer) {
        NaturalSpawnerData.canSpawnServerPlayer.set(serverplayer);
        return original;
    }
}
