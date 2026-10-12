package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.shaders.Program;
import org.confluence.lib.client.light.DynamicLightGpu;
import org.confluence.lib.client.light.DynamicLightShader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Program.class)
public abstract class ProgramMixin {
    @ModifyArg(method = "compileShaderInternal", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/preprocessor/GlslPreprocessor;process(Ljava/lang/String;)Ljava/util/List;"))
    private static String dynamicLight(String source, @Local(argsOnly = true) Program.Type type, @Local(argsOnly = true, ordinal = 0) String name) {
        return DynamicLightGpu.prepare() ? DynamicLightShader.patch(source, type == Program.Type.VERTEX, name) : source;
    }
}
