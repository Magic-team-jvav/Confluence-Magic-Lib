package org.confluence.lib.mixin.naturalspawner;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.MobCategory;
import org.confluence.lib.mixed.ILibChunkSpawnDataAccess;
import org.confluence.lib.util.NaturalSpawnerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobCategory.class)
public abstract class MobCategoryMixin implements ILibChunkSpawnDataAccess {
    @Unique
    private NaturalSpawnerUtils.ChunkSpawnData confluence$data = NaturalSpawnerUtils.ChunkSpawnData.DEFAULT;

    @Override
    public void confluence$setData(NaturalSpawnerUtils.ChunkSpawnData data) {
        this.confluence$data = data;
    }

    @Override
    public NaturalSpawnerUtils.ChunkSpawnData confluence$getData() {
        return confluence$data;
    }

    @ModifyReturnValue(method = "getMaxInstancesPerChunk", at = @At("RETURN"))
    private int mul(int original) {
        return confluence$getData().getCount(original);
    }
}
