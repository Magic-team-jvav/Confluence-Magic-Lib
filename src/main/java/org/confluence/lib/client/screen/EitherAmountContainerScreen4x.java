package org.confluence.lib.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.menu.EitherAmountContainerMenu4x;
import org.joml.Matrix3x2fStack;

public class EitherAmountContainerScreen4x<M extends EitherAmountContainerMenu4x<?, ?, ?, ?>> extends AbstractContainerScreen<M> {
    public static final Identifier BACKGROUND = ConfluenceMagicLib.asResource("textures/gui/container/normal4x.png");
    private float titleScale = 1;
    private boolean upButtonClicked = false;
    private boolean downButtonClicked = false;
    private Identifier background;

    public EitherAmountContainerScreen4x(M menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    protected void init() {
        super.init();
        int titleWidth = font.width(title);
        if (titleWidth > 68) {
            this.titleScale = 68.0F / titleWidth;
            this.titleLabelX = imageWidth - 76;
        } else {
            this.titleLabelX = imageWidth - titleWidth - 8;
        }
        this.inventoryLabelX = imageWidth - font.width(playerInventoryTitle) - 8;
        this.background = background();
    }

    protected Identifier background() {
        return BACKGROUND;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(titleLabelX, titleLabelY);
        pose.scale(titleScale, titleScale);
        graphics.text(font, title, 0, 0, 4210752, false);
        pose.popMatrix();
        graphics.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        extractTooltip(guiGraphics, mouseX, mouseY);
        menu.resultSlot.isActive = true;
        if (menu.getRecipesAmount() > 1) {
            String text = menu.getCurrentIndex() + 1 + "/" + menu.getRecipesAmount();
            guiGraphics.text(font, text, leftPos + 154, topPos + 37 + (16 - font.lineHeight) / 2, 4210752, false);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        graphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.getRecipesAmount() > 1) {
            if (upButtonClicked) {
                graphics.blit(background, leftPos + 135, topPos + 21, 188, 1, 10, 7);
            } else {
                graphics.blit(background, leftPos + 135, topPos + 20, 177, 0, 10, 8);
            }
            if (downButtonClicked) {
                graphics.blit(background, leftPos + 135, topPos + 61, 188, 10, 10, 7);
            } else {
                graphics.blit(background, leftPos + 135, topPos + 60, 177, 9, 10, 8);
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (menu.getRecipesAmount() > 1) {
            if (isOverUpButton((int) event.x() - leftPos, (int) event.y() - topPos)) {
                int upIndex = menu.getUpIndex();
                if (menu.clickMenuButton(minecraft.player, upIndex)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, upIndex);
                    this.upButtonClicked = true;
                    this.downButtonClicked = false;
                    return true;
                }
                return false;
            } else if (isOverDownButton((int) event.x() - leftPos, (int) event.y() - topPos)) {
                int downIndex = menu.getDownIndex();
                if (menu.clickMenuButton(minecraft.player, downIndex)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, downIndex);
                    this.upButtonClicked = false;
                    this.downButtonClicked = true;
                    return true;
                }
                return false;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.upButtonClicked = false;
        this.downButtonClicked = false;
        return super.mouseReleased(event);
    }

    private static boolean isOverUpButton(int x, int y) {
        return x >= 135 && x <= 145 && y >= 20 && y <= 28;
    }

    private static boolean isOverDownButton(int x, int y) {
        return x >= 135 && x <= 145 && y >= 60 && y <= 68;
    }
}
