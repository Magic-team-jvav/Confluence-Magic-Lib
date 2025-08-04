package org.confluence.lib.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.menu.EitherAmountContainerMenu4x;

public class EitherAmountContainerScreen4x<M extends EitherAmountContainerMenu4x<?, ?, ?, ?>> extends AbstractContainerScreen<M> {
    public static final ResourceLocation BACKGROUND = ConfluenceMagicLib.asResource("textures/gui/container/normal4x.png");
    private float titleScale = 1;
    private boolean upButtonClicked = false;
    private ItemStack upItem = null;
    private boolean downButtonClicked = false;
    private ItemStack downItem = null;
    private ResourceLocation background;

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

    protected ResourceLocation background() {
        return BACKGROUND;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate(titleLabelX, titleLabelY, 0);
        pose.scale(titleScale, titleScale, titleScale);
        guiGraphics.drawString(font, title, 0, 0, 4210752, false);
        pose.popPose();
        guiGraphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 4210752, false);
    }

    @Override
    public void render(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.render(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        renderTooltip(pGuiGraphics, pMouseX, pMouseY);
        menu.resultSlot.isActive = true;
        if (menu.getRecipesAmount() > 1) {
            if (isOverUpButton(pMouseX - leftPos, pMouseY - topPos)) {
                if (upItem == null) this.upItem = menu.getUpResult();
                pGuiGraphics.renderFakeItem(upItem, leftPos + 132, topPos + 36);
                this.downItem = null;
                menu.resultSlot.isActive = false;
            } else if (isOverDownButton(pMouseX - leftPos, pMouseY - topPos)) {
                if (downItem == null) this.downItem = menu.getDownResult();
                pGuiGraphics.renderFakeItem(downItem, leftPos + 132, topPos + 36);
                this.upItem = null;
                menu.resultSlot.isActive = false;
            }
            String text = menu.getCurrentIndex() + 1 + "/" + menu.getRecipesAmount();
            pGuiGraphics.drawString(font, text, leftPos + 154, topPos + 37 + (16 - font.lineHeight) / 2, 4210752, false);
        } else {
            this.upItem = null;
            this.downItem = null;
        }
    }

    @Override
    protected void renderBg(GuiGraphics pGuiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        pGuiGraphics.blit(background, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (menu.getRecipesAmount() > 1) {
            if (upButtonClicked) {
                pGuiGraphics.blit(background, leftPos + 135, topPos + 21, 188, 1, 10, 7);
            } else {
                pGuiGraphics.blit(background, leftPos + 135, topPos + 20, 177, 0, 10, 8);
            }
            if (downButtonClicked) {
                pGuiGraphics.blit(background, leftPos + 135, topPos + 61, 188, 10, 10, 7);
            } else {
                pGuiGraphics.blit(background, leftPos + 135, topPos + 60, 177, 9, 10, 8);
            }
        }
    }

    @Override
    public boolean mouseClicked(double pMouseX, double pMouseY, int pButton) {
        if (menu.getRecipesAmount() > 1) {
            if (isOverUpButton((int) pMouseX - leftPos, (int) pMouseY - topPos)) {
                int upIndex = menu.getUpIndex();
                if (menu.clickMenuButton(minecraft.player, upIndex)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, upIndex);
                    this.upButtonClicked = true;
                    this.downButtonClicked = false;
                    this.upItem = null;
                    return true;
                }
                return false;
            } else if (isOverDownButton((int) pMouseX - leftPos, (int) pMouseY - topPos)) {
                int downIndex = menu.getDownIndex();
                if (menu.clickMenuButton(minecraft.player, downIndex)) {
                    minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    minecraft.gameMode.handleInventoryButtonClick((this.menu).containerId, downIndex);
                    this.upButtonClicked = false;
                    this.downButtonClicked = true;
                    this.downItem = null;
                    return true;
                }
                return false;
            }
        }
        return super.mouseClicked(pMouseX, pMouseY, pButton);
    }

    @Override
    public boolean mouseReleased(double pMouseX, double pMouseY, int pButton) {
        this.upButtonClicked = false;
        this.downButtonClicked = false;
        return super.mouseReleased(pMouseX, pMouseY, pButton);
    }

    private static boolean isOverUpButton(int x, int y) {
        return x >= 135 && x <= 145 && y >= 20 && y <= 28;
    }

    private static boolean isOverDownButton(int x, int y) {
        return x >= 135 && x <= 145 && y >= 60 && y <= 68;
    }
}
