package org.confluence.lib.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.GroupItem;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

public enum GroupItemSpecialRenderer implements SpecialModelRenderer<GroupItem.Stacks> {
    INSTANCE;

    private final ItemStackRenderState state = new ItemStackRenderState();

    @Override
    public void submit(GroupItem.@Nullable Stacks argument, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, int overlayCoords, boolean hasFoil, int outlineColor) {
        if (argument == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        long time = minecraft.level == null ? System.currentTimeMillis() / 1000 : minecraft.level.getGameTime() / 40;
        ItemStack itemStack = argument.getCurrentRendered(time);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        minecraft.getItemModelResolver().updateForTopItem(state, itemStack, ItemDisplayContext.GUI, minecraft.level, null, 260726);
        state.submit(poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
        poseStack.popPose();
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {}

    @Override
    public GroupItem.@Nullable Stacks extractArgument(ItemStack stack) {
        return stack.get(ConfluenceMagicLib.GROUP_STACKS);
    }

    public enum Unbaked implements SpecialModelRenderer.Unbaked<GroupItem.Stacks> {
        INSTANCE;

        public static final Identifier ID = ConfluenceMagicLib.asResource("group");
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public GroupItemSpecialRenderer bake(SpecialModelRenderer.BakingContext context) {
            return GroupItemSpecialRenderer.INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
