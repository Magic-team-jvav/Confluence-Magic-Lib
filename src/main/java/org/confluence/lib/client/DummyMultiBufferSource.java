package org.confluence.lib.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;

public class DummyMultiBufferSource implements MultiBufferSource {
    public static final MultiBufferSource INSTANCE = new DummyMultiBufferSource();

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        return DummyVertexConsumer.INSTANCE;
    }

    public static boolean isDummy(MultiBufferSource buffer) {
        return buffer == INSTANCE;
    }
}
