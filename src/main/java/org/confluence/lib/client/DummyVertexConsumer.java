package org.confluence.lib.client;

import com.mojang.blaze3d.vertex.VertexConsumer;

public class DummyVertexConsumer implements VertexConsumer {
    public static final VertexConsumer INSTANCE = new DummyVertexConsumer();

    @Override
    public VertexConsumer vertex(double v, double v1, double v2) {
        return this;
    }

    @Override
    public VertexConsumer color(int i, int i1, int i2, int i3) {
        return this;
    }

    @Override
    public VertexConsumer uv(float v, float v1) {
        return this;
    }

    @Override
    public VertexConsumer overlayCoords(int i, int i1) {
        return this;
    }

    @Override
    public VertexConsumer uv2(int i, int i1) {
        return this;
    }

    @Override
    public VertexConsumer normal(float v, float v1, float v2) {
        return this;
    }

    @Override
    public void endVertex() {}

    @Override
    public void defaultColor(int i, int i1, int i2, int i3) {}

    @Override
    public void unsetDefaultColor() {}
}
