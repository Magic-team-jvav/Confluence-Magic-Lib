package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.entity.Entity;
import org.confluence.lib.mixed.ILibEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Entity.class)
public abstract class ClientEntityMixin implements ILibEntity {
    @ModifyReturnValue(method = "getEyeHeight()F", at = @At("RETURN"))
    private float eyeHeight(float original) {
        if (confluence$isShouldRot()) {
            return confluence$getDimensionHeight() * 0.15F;
        }
        return original;
    }
}
