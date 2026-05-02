package org.confluence.lib.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.GroupItem;

public enum GroupItemExtension implements IClientItemExtensions {
    INSTANCE;

    private BlockEntityWithoutLevelRenderer renderer;

    @Override
    public BlockEntityWithoutLevelRenderer getCustomRenderer() {
        if (renderer == null) {
            Minecraft minecraft = Minecraft.getInstance();
            this.renderer = new BlockEntityWithoutLevelRenderer(minecraft.getBlockEntityRenderDispatcher(), minecraft.getEntityModels()) {
                @Override
                public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
                    GroupItem.Stacks stacks = stack.get(ConfluenceMagicLib.GROUP_STACKS);
                    if (stacks == null) return;
                    long time = minecraft.level == null ? System.currentTimeMillis() / 1000 : minecraft.level.getGameTime() / 40;
                    ItemStack itemStack = stacks.getCurrentRendered(time);
                    BakedModel bakedModel = minecraft.getItemRenderer().getModel(itemStack, minecraft.level, minecraft.player, 251014);

                    poseStack.pushPose();
                    poseStack.translate(0.5F, 0.5F, 0.5F);
                    minecraft.getItemRenderer().render(itemStack, displayContext, false, poseStack, buffer, packedLight, packedOverlay, bakedModel);
                    poseStack.popPose();
                }
            };
        }
        return renderer;
    }
}
