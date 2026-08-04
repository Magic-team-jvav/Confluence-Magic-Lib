package org.confluence.lib.mixin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.mixed.LibGuiGraphicsExtractorExtension;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2fStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorMixin implements LibGuiGraphicsExtractorExtension {
    @Unique
    private static final Identifier confluence$wip = ConfluenceMagicLib.asResource("textures/gui/wip.png");

    @Shadow
    @Final
    private Matrix3x2fStack pose;

    @Inject(method = "itemDecorations(Lnet/minecraft/client/gui/Font;Lnet/minecraft/world/item/ItemStack;IILjava/lang/String;)V", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix3x2fStack;pushMatrix()Lorg/joml/Matrix3x2fStack;"))
    private void renderItemDecorations(Font font, ItemStack itemStack, int x, int y, @Nullable String countText, CallbackInfo ci) {
        if (itemStack.is(LibTags.Items.WIP)) {
            pose.pushMatrix();
            pose.translate(0.0F, 0.0F);
            blit(confluence$wip, x, y, 0.0F, 0.0F, 16, 16, 16, 16);
            pose.popMatrix();
        }
    }
}
