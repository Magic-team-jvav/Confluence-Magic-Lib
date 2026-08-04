package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.confluence.lib.common.data.saved.IGlobalData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PrimaryLevelData.class)
public abstract class PrimaryLevelDataMixin {
    @Inject(method = "parse", at = @At("TAIL"))
    private static <T> void decode(CallbackInfoReturnable<PrimaryLevelData> cir, @Local(argsOnly = true, name = "input") Dynamic<T> input) {
        for (IGlobalData data : IGlobalData.DAT) {
            data.decode(input.get(data.serializeKey()).orElseEmptyMap());
        }
    }

    @Inject(method = "setTagData", at = @At("TAIL"))
    private void encode(CallbackInfo ci, @Local(argsOnly = true, name = "tag") CompoundTag tag) {
        for (IGlobalData data : IGlobalData.DAT) {
            CompoundTag nbt = new CompoundTag();
            data.encode(nbt);
            tag.put(data.serializeKey(), nbt);
        }
    }
}
