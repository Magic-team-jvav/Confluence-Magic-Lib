package org.confluence.lib.client.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.client.DPSMeter;
import org.confluence.lib.client.animate.ExpertColorAnimation;
import org.confluence.lib.client.animate.MasterColorAnimation;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.integration.animation.AnimationConstants;
import org.confluence.lib.integration.animation.PlayerAttackingStatePacket;

import java.util.List;
import java.util.Optional;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)
public final class LibGameEvents {
    @SubscribeEvent
    public static void clientTick$Post(ClientTickEvent.Pre event) {
        ExpertColorAnimation.INSTANCE.updateColor();
        MasterColorAnimation.INSTANCE.updateColor();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            DPSMeter.checkDPSTime(player.level().getGameTime());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void renderTooltip$GatherComponents(RenderTooltipEvent.GatherComponents event) {
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty()) return;

        List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
        if (tooltipElements.isEmpty()) return;
        Optional<FormattedText> displayName = tooltipElements.getFirst().left();
        if (displayName.isPresent() && displayName.get() instanceof Component component) {
            ModRarity rarity = ModRarity.getRarity(stack);
            if (rarity != null) {
                tooltipElements.set(0, Either.left(component.copy().withColor(rarity.color())));
            }
        }

        if (stack.is(LibTags.Items.WIP)) {
            event.getTooltipElements().add(1, Either.left(Component.translatable("tooltip.confluence.work_in_progress").withStyle(ChatFormatting.RED)));
        }
    }

    @SubscribeEvent
    public static void input$InteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (AnimationConstants.SHOULD_APPLY && event.isAttack()) {
            PlayerAttackingStatePacket.sendToServer();
        }
    }
}
