package org.confluence.lib.mixed;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

public interface LibGuiGraphicsExtractorExtension {
    private GuiGraphicsExtractor self() {
        return (GuiGraphicsExtractor) this;
    }

    default void blit(Identifier atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height, int textureWidth, int textureHeight) {
        self().blit(RenderPipelines.GUI_TEXTURED, atlasLocation, x, y, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    default void blit(Identifier atlasLocation, int x, int y, float uOffset, float vOffset, int width, int height) {
        blit(atlasLocation, x, y, uOffset, vOffset, width, height, 256, 256);
    }

    default void blitSprite(Identifier sprite, int textureWidth, int textureHeight, int uPosition, int vPosition, int x, int y, int uWidth, int vHeight) {
        self().blitSprite(RenderPipelines.GUI_TEXTURED, sprite, textureWidth, textureHeight, uPosition, vPosition, x, y, uWidth, vHeight);
    }

    default void drawString(Font font, Component text, int x, int y, int color) {
        self().text(font, text, x, y, color);
    }

    default void drawString(Font font, Component text, int x, int y, int color, boolean shadow) {
        self().text(font, text, x, y, color, shadow);
    }

    default void drawString(Font font, String text, int x, int y, int color) {
        self().text(font, text, x, y, color);
    }

    default void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        self().text(font, text, x, y, color, shadow);
    }

    default void renderFakeItem(ItemStack stack, int x, int y) {
        self().fakeItem(stack, x, y);
    }

    default void renderOutline(int x, int y, int w, int h, int c) {
        self().outline(x, y, w, h, c);
    }
}
