package org.confluence.lib.client.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class LibGameEvents {
    @SubscribeEvent
    public static void renderTooltip$GatherComponents(RenderTooltipEvent.GatherComponents event) {
        if (event.getItemStack().isEmpty() || !event.getItemStack().is(LibTags.Items.WIP)) return;
        event.getTooltipElements().add(1, Either.left(Component.translatable("tooltip.confluence.work_in_progress").withStyle(ChatFormatting.RED)));
    }
}
