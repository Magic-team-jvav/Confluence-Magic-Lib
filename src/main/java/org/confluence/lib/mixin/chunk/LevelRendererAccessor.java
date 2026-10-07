package org.confluence.lib.mixin.chunk;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {
    @Invoker("setSectionDirty")
    void callSetSectionDirty(int sectionX, int sectionY, int sectionZ, boolean reRenderOnMainThread);
}
