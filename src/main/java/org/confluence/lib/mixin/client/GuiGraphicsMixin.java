package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique
    private static final ResourceLocation confluence$wip = ConfluenceMagicLib.asResource("wip");

    @Shadow
    @Final
    private PoseStack pose;

    @Shadow
    public abstract void blitSprite(ResourceLocation sprite, int x, int y, int width, int height);

    @Inject(method = "renderItemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V"))
    private void renderItemDecorations(CallbackInfo ci, @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true, ordinal = 0) int x, @Local(argsOnly = true, ordinal = 1) int y) {
        if (stack.is(LibTags.Items.WIP)) {
            pose.pushPose();
            pose.translate(0.0F, 0.0F, 200.0F);
            blitSprite(confluence$wip, x, y, 16, 16);
            pose.popPose();
        }
    }
}
