package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.confluence.lib.mixed.ILibEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @ModifyReturnValue(method = "isEntityUpsideDown", at = @At(value = "RETURN", ordinal = 1))
    private static boolean upsideDown(boolean original, @Local(argsOnly = true) LivingEntity living) {
        if (!original && ILibEntity.of(living).confluence$isShouldRot()) {
            return true;
        }
        return original;
    }
}
