package org.confluence.lib.client.event;

import PortLib.extensions.net.minecraft.network.chat.MutableComponent.PortMutableComponentExtension;
import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.client.DPSMeter;
import org.confluence.lib.client.animate.ExpertColorAnimation;
import org.confluence.lib.client.animate.MasterColorAnimation;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.common.component.ModRarity;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.client.PortClientTickEvent;
import org.mesdag.portlib.event.client.PortRenderTooltipEvent;

import java.util.List;
import java.util.Optional;

public final class LibGameEvents {
    public static void init() {
        PortEventHandler.addListener(LibGameEvents::clientTick);
        PortEventHandler.addListener(PortEventPriority.HIGHEST, LibGameEvents::renderTooltip);
    }

    private static void clientTick(PortClientTickEvent.PortPre event) {
        ExpertColorAnimation.INSTANCE.updateColor();
        MasterColorAnimation.INSTANCE.updateColor();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            DPSMeter.checkDPSTime(player.level().getGameTime());
        }
    }

    private static void renderTooltip(PortRenderTooltipEvent.PortGatherComponents event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) return;

        List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
        if (tooltipElements.isEmpty()) return;
        Optional<FormattedText> displayName = tooltipElements.get(0).left();
        if (displayName.isPresent() && displayName.get() instanceof Component component) {
            ModRarity rarity = ModRarity.getRarity(itemStack);
            if (rarity != null) {
                tooltipElements.set(0, Either.left(PortMutableComponentExtension.withColor(component.copy(), rarity.color())));
            }
        }

        if (itemStack.is(LibTags.Items.WIP)) {
            event.getTooltipElements().add(1, Either.left(Component.translatable("tooltip.confluence.work_in_progress").withStyle(ChatFormatting.RED)));
        }
    }
}
