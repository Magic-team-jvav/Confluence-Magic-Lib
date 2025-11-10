package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
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

    @Definition(id = "playerMobCounts", field = "Lnet/minecraft/world/level/LocalMobCapCalculator;playerMobCounts:Ljava/util/Map;")
    @Definition(id = "get", method = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;")
    @Definition(id = "serverplayer", local = @Local(type = ServerPlayer.class))
    @Expression("this.playerMobCounts.get(serverplayer)")
    @ModifyExpressionValue(method = "canSpawn", at = @At("MIXINEXTRAS:EXPRESSION"))
    private <V> V confluenceLib$canSpawn(final V original,
                                         @Local(argsOnly = true) MobCategory category,
                                         @Local(ordinal = 0) ServerPlayer serverplayer) {
        NaturalSpawnerData.canSpawnServerPlayer.set(serverplayer);
        return original;
    }
}
