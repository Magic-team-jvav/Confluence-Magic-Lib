package org.confluence.lib.util;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.resources.ResourceLocation;

import static java.lang.Math.min;

/**
 * GUI工具类，提供各种绘制精灵图的方法
 */
public final class LibGuiUtil {
    /**
     * 绘制精灵图
     *
     * @param guiGraphics GUI图形上下文
     * @param sprite      精灵图资源位置
     * @param x           绘制位置x坐标
     * @param y           绘制位置y坐标
     * @param width       绘制宽度
     * @param height      绘制高度
     */
    public static void blitSprite(GuiGraphics guiGraphics, ResourceLocation sprite, float x, float y, float width, float height) {
        blitSprite(guiGraphics, sprite, x, y, 0f, width, height);
    }

    /**
     * 绘制精灵图
     *
     * @param guiGraphics GUI图形上下文
     * @param sprite      精灵图资源位置
     * @param x           绘制位置x坐标
     * @param y           绘制位置y坐标
     * @param blitOffset  绘制偏移量
     * @param width       绘制宽度
     * @param height      绘制高度
     */
    public static void blitSprite(GuiGraphics guiGraphics, ResourceLocation sprite, float x, float y, float blitOffset, float width, float height) {
        TextureAtlasSprite textureAtlasSprite = guiGraphics.sprites.getSprite(sprite);
        GuiSpriteScaling guiSpriteScaling = guiGraphics.sprites.getSpriteScaling(textureAtlasSprite);

        switch (guiSpriteScaling) {
            case GuiSpriteScaling.Stretch stretch ->
                    blitSprite(guiGraphics, textureAtlasSprite, x, y, blitOffset, width, height);
            case GuiSpriteScaling.Tile(int width1, int height1) -> {
                float spriteWidth = (float) width1;
                float spriteHeight = (float) height1;
                blitTiledSprite(
                        guiGraphics,
                        textureAtlasSprite,
                        x,
                        y,
                        blitOffset,
                        width,
                        height,
                        0f,
                        0f,
                        spriteWidth,
                        spriteHeight,
                        spriteWidth,
                        spriteHeight
                );
            }
            case GuiSpriteScaling.NineSlice nineSlice -> blitNineSlicedSprite(
                    guiGraphics,
                    textureAtlasSprite,
                    nineSlice,
                    x,
                    y,
                    blitOffset,
                    width,
                    height
            );
            default -> {
            }
        }
    }

    /**
     * 绘制九宫格缩放精灵图
     *
     * @param guiGraphics GUI图形上下文
     * @param sprite      纹理图集精灵
     * @param nineSlice   九宫格缩放信息
     * @param x           绘制位置x坐标
     * @param y           绘制位置y坐标
     * @param blitOffset  绘制偏移量
     * @param width       绘制宽度
     * @param height      绘制高度
     */
    public static void blitNineSlicedSprite(
            GuiGraphics guiGraphics,
            TextureAtlasSprite sprite,
            GuiSpriteScaling.NineSlice nineSlice,
            float x,
            float y,
            float blitOffset,
            float width,
            float height
    ) {
        var border = nineSlice.border();
        float i = min((float) border.left(), width / 2);
        float j = min((float) border.right(), width / 2);
        float k = min((float) border.top(), height / 2);
        float l = min((float) border.bottom(), height / 2);

        if (width == (float) nineSlice.width() && height == (float) nineSlice.height()) {
            blitSprite(
                    guiGraphics,
                    sprite,
                    (float) nineSlice.width(),
                    (float) nineSlice.height(),
                    0f,
                    0f,
                    x,
                    y,
                    blitOffset,
                    width,
                    height
            );
            return;
        }

        // 处理高度相等的情况
        if (height == (float) nineSlice.height()) {
            blitSprite(
                    guiGraphics,
                    sprite,
                    (float) nineSlice.width(),
                    (float) nineSlice.height(),
                    0f,
                    0f,
                    x,
                    y,
                    blitOffset,
                    i,
                    height
            );
            blitTiledSprite(
                    guiGraphics,
                    sprite,
                    x + i,
                    y,
                    blitOffset,
                    width - j - i,
                    height,
                    i,
                    0f,
                    (float) nineSlice.width() - j - i,
                    (float) nineSlice.height(),
                    (float) nineSlice.width(),
                    (float) nineSlice.height()
            );
            blitSprite(
                    guiGraphics,
                    sprite,
                    (float) nineSlice.width(),
                    (float) nineSlice.height(),
                    (float) nineSlice.width() - j,
                    0f,
                    x + width - j,
                    y,
                    blitOffset,
                    j,
                    height
            );
            return;
        }

        // 处理宽度相等的情况
        if (width == (float) nineSlice.width()) {
            blitSprite(
                    guiGraphics,
                    sprite,
                    (float) nineSlice.width(),
                    (float) nineSlice.height(),
                    0f,
                    0f,
                    x,
                    y,
                    blitOffset,
                    width,
                    k
            );
            blitTiledSprite(
                    guiGraphics,
                    sprite,
                    x,
                    y + k,
                    blitOffset,
                    width,
                    height - l - k,
                    0f,
                    k,
                    (float) nineSlice.width(),
                    (float) nineSlice.height() - l - k,
                    (float) nineSlice.width(),
                    (float) nineSlice.height()
            );
            blitSprite(
                    guiGraphics,
                    sprite,
                    (float) nineSlice.width(),
                    (float) nineSlice.height(),
                    0f,
                    (float) nineSlice.height() - l,
                    x,
                    y + height - l,
                    blitOffset,
                    width,
                    l
            );
            return;
        }

        // 处理一般情况，分别绘制九个区域
        blitSprite(
                guiGraphics,
                sprite,
                (float) nineSlice.width(),
                (float) nineSlice.height(),
                0f,
                0f,
                x,
                y,
                blitOffset,
                i,
                k
        );
        blitTiledSprite(
                guiGraphics,
                sprite,
                x + i,
                y,
                blitOffset,
                width - j - i,
                k,
                i,
                0f,
                (float) nineSlice.width() - j - i,
                k,
                (float) nineSlice.width(),
                (float) nineSlice.height()
        );
        blitSprite(
                guiGraphics,
                sprite,
                (float) nineSlice.width(),
                (float) nineSlice.height(),
                (float) nineSlice.width() - j,
                0f,
                x + width - j,
                y,
                blitOffset,
                j,
                k
        );
        blitSprite(
                guiGraphics,
                sprite,
                (float) nineSlice.width(),
                (float) nineSlice.height(),
                0f,
                (float) nineSlice.height() - l,
                x,
                y + height - l,
                blitOffset,
                i,
                l
        );
        blitTiledSprite(
                guiGraphics,
                sprite,
                x + i,
                y + height - l,
                blitOffset,
                width - j - i,
                l,
                i,
                (float) nineSlice.height() - l,
                (float) nineSlice.width() - j - i,
                l,
                (float) nineSlice.width(),
                (float) nineSlice.height()
        );
        blitSprite(
                guiGraphics,
                sprite,
                (float) nineSlice.width(),
                (float) nineSlice.height(),
                (float) nineSlice.width() - j,
                (float) nineSlice.height() - l,
                x + width - j,
                y + height - l,
                blitOffset,
                j,
                l
        );
        blitTiledSprite(
                guiGraphics,
                sprite,
                x,
                y + k,
                blitOffset,
                i,
                height - l - k,
                0f,
                k,
                i,
                (float) nineSlice.height() - l - k,
                (float) nineSlice.width(),
                (float) nineSlice.height()
        );
        blitTiledSprite(
                guiGraphics,
                sprite,
                x + i,
                y + k,
                blitOffset,
                width - j - i,
                height - l - k,
                i,
                k,
                (float) nineSlice.width() - j - i,
                (float) nineSlice.height() - l - k,
                (float) nineSlice.width(),
                (float) nineSlice.height()
        );
        blitTiledSprite(
                guiGraphics,
                sprite,
                x + width - j,
                y + k,
                blitOffset,
                i,
                height - l - k,
                (float) nineSlice.width() - j,
                k,
                j,
                (float) nineSlice.height() - l - k,
                (float) nineSlice.width(),
                (float) nineSlice.height()
        );
    }

    /**
     * 平铺绘制精灵图
     *
     * @param guiGraphics     GUI图形上下文
     * @param sprite          纹理图集精灵
     * @param x               绘制位置x坐标
     * @param y               绘制位置y坐标
     * @param blitOffset      绘制偏移量
     * @param width           绘制宽度
     * @param height          绘制高度
     * @param uPosition       UV坐标的u起始位置
     * @param vPosition       UV坐标的v起始位置
     * @param spriteWidth     精灵图宽度
     * @param spriteHeight    精灵图高度
     * @param nineSliceWidth  九宫格切片宽度
     * @param nineSliceHeight 九宫格切片高度
     */
    public static void blitTiledSprite(
            GuiGraphics guiGraphics,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float blitOffset,
            float width,
            float height,
            float uPosition,
            float vPosition,
            float spriteWidth,
            float spriteHeight,
            float nineSliceWidth,
            float nineSliceHeight
    ) {
        if (width <= 0 || height <= 0) {
            return;
        }

        if (spriteWidth <= 0 || spriteHeight <= 0) {
            throw new IllegalArgumentException("Tiled sprite texture size must be positive, got " + spriteWidth + "x" + spriteHeight);
        }

        float i = 0f;
        while (i < width) {
            float j = min(spriteWidth, width - i);

            float k = 0f;
            while (k < height) {
                float l = min(spriteHeight, height - k);
                blitSprite(
                        guiGraphics,
                        sprite,
                        nineSliceWidth,
                        nineSliceHeight,
                        uPosition,
                        vPosition,
                        x + i,
                        y + k,
                        blitOffset,
                        j,
                        l
                );
                k += spriteHeight;
            }
            i += spriteWidth;
        }
    }

    /**
     * 绘制精灵图
     *
     * @param guiGraphics   GUI图形上下文
     * @param sprite        精灵图资源位置
     * @param textureWidth  纹理宽度
     * @param textureHeight 纹理高度
     * @param uPosition     UV坐标的u起始位置
     * @param vPosition     UV坐标的v起始位置
     * @param x             绘制位置x坐标
     * @param y             绘制位置y坐标
     * @param uWidth        UV坐标的u宽度
     * @param vHeight       UV坐标的v高度
     */
    public static void blitSprite(
            GuiGraphics guiGraphics,
            ResourceLocation sprite,
            float textureWidth,
            float textureHeight,
            float uPosition,
            float vPosition,
            float x,
            float y,
            float uWidth,
            float vHeight
    ) {
        blitSprite(guiGraphics, sprite, textureWidth, textureHeight, uPosition, vPosition, x, y, 0f, uWidth, vHeight);
    }

    /**
     * 绘制精灵图
     *
     * @param guiGraphics   GUI图形上下文
     * @param sprite        精灵图资源位置
     * @param textureWidth  纹理宽度
     * @param textureHeight 纹理高度
     * @param uPosition     UV坐标的u起始位置
     * @param vPosition     UV坐标的v起始位置
     * @param x             绘制位置x坐标
     * @param y             绘制位置y坐标
     * @param blitOffset    绘制偏移量
     * @param uWidth        UV坐标的u宽度
     * @param vHeight       UV坐标的v高度
     */
    public static void blitSprite(
            GuiGraphics guiGraphics,
            ResourceLocation sprite,
            float textureWidth,
            float textureHeight,
            float uPosition,
            float vPosition,
            float x,
            float y,
            float blitOffset,
            float uWidth,
            float vHeight
    ) {
        TextureAtlasSprite textureAtlasSprite = guiGraphics.sprites.getSprite(sprite);
        GuiSpriteScaling guiSpriteScaling = guiGraphics.sprites.getSpriteScaling(textureAtlasSprite);

        if (guiSpriteScaling instanceof GuiSpriteScaling.Stretch) {
            blitSprite(
                    guiGraphics,
                    textureAtlasSprite,
                    textureWidth,
                    textureHeight,
                    uPosition,
                    vPosition,
                    x,
                    y,
                    blitOffset,
                    uWidth,
                    vHeight
            );
            return;
        }

        blitSprite(guiGraphics, textureAtlasSprite, x, y, blitOffset, uWidth, vHeight);
    }

    /**
     * 绘制精灵图
     *
     * @param guiGraphics   GUI图形上下文
     * @param sprite        纹理图集精灵
     * @param textureWidth  纹理宽度
     * @param textureHeight 纹理高度
     * @param uPosition     UV坐标的u起始位置
     * @param vPosition     UV坐标的v起始位置
     * @param x             绘制位置x坐标
     * @param y             绘制位置y坐标
     * @param blitOffset    绘制偏移量
     * @param uWidth        UV坐标的u宽度
     * @param vHeight       UV坐标的v高度
     */
    public static void blitSprite(
            GuiGraphics guiGraphics,
            TextureAtlasSprite sprite,
            float textureWidth,
            float textureHeight,
            float uPosition,
            float vPosition,
            float x,
            float y,
            float blitOffset,
            float uWidth,
            float vHeight
    ) {
        if (uWidth == 0f || vHeight == 0f) {
            return;
        }
        innerBlit(
                guiGraphics,
                sprite.atlasLocation(),
                x,
                x + uWidth,
                y,
                y + vHeight,
                blitOffset,
                sprite.getU(uPosition / textureWidth),
                sprite.getU((uPosition + uWidth) / textureWidth),
                sprite.getV(vPosition / textureHeight),
                sprite.getV((vPosition + vHeight) / textureHeight)
        );
    }

    /**
     * 内部绘制方法，执行实际的顶点绘制操作
     *
     * @param guiGraphics   GUI图形上下文
     * @param atlasLocation 图集资源位置
     * @param x1            左侧x坐标
     * @param x2            右侧x坐标
     * @param y1            上方y坐标
     * @param y2            下方y坐标
     * @param blitOffset    绘制偏移量
     * @param minU          最小U坐标
     * @param maxU          最大U坐标
     * @param minV          最小V坐标
     * @param maxV          最大V坐标
     */
    private static void innerBlit(
            GuiGraphics guiGraphics,
            ResourceLocation atlasLocation,
            float x1,
            float x2,
            float y1,
            float y2,
            float blitOffset,
            float minU,
            float maxU,
            float minV,
            float maxV
    ) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        var matrix4f = guiGraphics.pose().last().pose();
        var bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferBuilder.addVertex(matrix4f, x1, y1, blitOffset).setUv(minU, minV);
        bufferBuilder.addVertex(matrix4f, x1, y2, blitOffset).setUv(minU, maxV);
        bufferBuilder.addVertex(matrix4f, x2, y2, blitOffset).setUv(maxU, maxV);
        bufferBuilder.addVertex(matrix4f, x2, y1, blitOffset).setUv(maxU, minV);
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }

    /**
     * 绘制精灵图
     *
     * @param guiGraphics GUI图形上下文
     * @param sprite      纹理图集精灵
     * @param x           绘制位置x坐标
     * @param y           绘制位置y坐标
     * @param blitOffset  绘制偏移量
     * @param width       绘制宽度
     * @param height      绘制高度
     */
    public static void blitSprite(
            GuiGraphics guiGraphics,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float blitOffset,
            float width,
            float height
    ) {
        if (width == 0f || height == 0f) {
            return;
        }
        innerBlit(
                guiGraphics,
                sprite.atlasLocation(),
                x,
                x + width,
                y,
                y + height,
                blitOffset,
                sprite.getU0(),
                sprite.getU1(),
                sprite.getV0(),
                sprite.getV1()
        );
    }

    public static void fill(GuiGraphics guiGraphics, float minX, float minY, float maxX, float maxY, int color) {
        fill(guiGraphics, minX, minY, maxX, maxY, 0f, color);
    }

    public static void fill(GuiGraphics guiGraphics, float minX, float minY, float maxX, float maxY, float z, int color) {
        fill(guiGraphics, RenderType.gui(), minX, minY, maxX, maxY, z, color);
    }

    public static void fill(
            GuiGraphics guiGraphics,
            RenderType renderType,
            float minX,
            float minY,
            float maxX,
            float maxY,
            float z,
            int color
    ) {
        var matrix4f = guiGraphics.pose().last().pose();

        if (minX < maxX) {
            float temp = minX;
            minX = maxX;
            maxX = temp;
        }

        if (minY < maxY) {
            float temp = minY;
            minY = maxY;
            maxY = temp;
        }

        var vertexConsumer = guiGraphics.bufferSource().getBuffer(renderType);
        vertexConsumer.addVertex(matrix4f, minX, minY, z).setColor(color);
        vertexConsumer.addVertex(matrix4f, minX, maxY, z).setColor(color);
        vertexConsumer.addVertex(matrix4f, maxX, maxY, z).setColor(color);
        vertexConsumer.addVertex(matrix4f, maxX, minY, z).setColor(color);
        guiGraphics.flushIfUnmanaged();
    }

    public static void fill(
            GuiGraphics guiGraphics,
            RenderType renderType,
            float minX,
            float minY,
            float maxX,
            float maxY,
            int color
    ) {
        fill(guiGraphics, renderType, minX, minY, maxX, maxY, 0f, color);
    }

    public static void fillGradient(
            GuiGraphics guiGraphics,
            float x1,
            float y1,
            float x2,
            float y2,
            int colorFrom,
            int colorTo
    ) {
        fillGradient(guiGraphics, x1, y1, x2, y2, 0f, colorFrom, colorTo);
    }

    public static void fillGradient(
            GuiGraphics guiGraphics,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            int colorFrom,
            int colorTo
    ) {
        fillGradient(guiGraphics, RenderType.gui(), x1, y1, x2, y2, colorFrom, colorTo, z);
    }

    public static void fillGradient(
            GuiGraphics guiGraphics,
            RenderType renderType,
            float x1,
            float y1,
            float x2,
            float y2,
            int colorFrom,
            int colorTo,
            float z
    ) {
        var vertexConsumer = guiGraphics.bufferSource().getBuffer(renderType);
        fillGradient(guiGraphics, vertexConsumer, x1, y1, x2, y2, z, colorFrom, colorTo);
        guiGraphics.flushIfUnmanaged();
    }

    public static void fillGradient(
            GuiGraphics guiGraphics,
            VertexConsumer consumer,
            float x1,
            float y1,
            float x2,
            float y2,
            float z,
            int colorFrom,
            int colorTo
    ) {
        var matrix4f = guiGraphics.pose().last().pose();
        consumer.addVertex(matrix4f, x1, y1, z).setColor(colorFrom);
        consumer.addVertex(matrix4f, x1, y2, z).setColor(colorTo);
        consumer.addVertex(matrix4f, x2, y2, z).setColor(colorTo);
        consumer.addVertex(matrix4f, x2, y1, z).setColor(colorFrom);
    }

    public static void fillRenderType(
            GuiGraphics guiGraphics,
            RenderType renderType,
            float x1,
            float y1,
            float x2,
            float y2,
            float z
    ) {
        var matrix4f = guiGraphics.pose().last().pose();
        var vertexConsumer = guiGraphics.bufferSource().getBuffer(renderType);
        vertexConsumer.addVertex(matrix4f, x1, y1, z);
        vertexConsumer.addVertex(matrix4f, x1, y2, z);
        vertexConsumer.addVertex(matrix4f, x2, y2, z);
        vertexConsumer.addVertex(matrix4f, x2, y1, z);
        guiGraphics.flushIfUnmanaged();
    }

    public static void lineRenderType(
            GuiGraphics guiGraphics,
            RenderType renderType,
            float x1,
            float y1,
            float x2,
            float y2,
            float width,
            float z
    ) {
        var matrix4f = guiGraphics.pose().last().pose();
        var vertexConsumer = guiGraphics.bufferSource().getBuffer(renderType);

        // 计算线条的方向向量
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);

        if (len == 0) {
            return; // 避免除以零
        }

        // 计算垂直于线条方向的单位向量
        float nx = -dy / len * width / 2;
        float ny = dx / len * width / 2;

        // 绘制矩形的四个顶点（将线条视为有宽度的矩形）
        vertexConsumer.addVertex(matrix4f, x1 - nx, y1 - ny, z);
        vertexConsumer.addVertex(matrix4f, x1 + nx, y1 + ny, z);
        vertexConsumer.addVertex(matrix4f, x2 + nx, y2 + ny, z);
        vertexConsumer.addVertex(matrix4f, x2 - nx, y2 - ny, z);

        guiGraphics.flushIfUnmanaged();
    }

    /**
     * 使用 Bresenham 直线算法绘制像素线。
     * 该函数通过计算像素点路径并逐个填充的方式，在两点之间绘制直线。
     *
     * @param guiGraphics 用于渲染的 GuiGraphics 上下文对象
     * @param x0          起点的 X 坐标
     * @param y0          起点的 Y 坐标
     * @param x1          终点的 X 坐标
     * @param y1          终点的 Y 坐标
     * @param width       线条宽度（当前实现固定为单像素绘制，此参数暂未参与计算）
     * @param color       线条的颜色值
     */
    public static void drawPixelLine(GuiGraphics guiGraphics, int x0, int y0, int x1, int y1, float width, int color) {
        // 初始化 Bresenham 算法所需的差值和步进方向
        int dx = Math.abs(x1 - x0); // 横坐标差
        int sx = x0 < x1 ? 1 : -1; // 横坐标步进方向
        int dy = -Math.abs(y1 - y0); // 纵坐标差
        int sy = y0 < y1 ? 1 : -1; // 纵坐标步进方向
        // 初始化误差累积项
        int err = dx + dy;

        // 遍历路径上的每个像素点
        while (true) {
            guiGraphics.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) {
                break;
            }

            // 根据误差项决定在 X 轴或 Y 轴方向的步进
            int e2 = err * 2;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }
}
