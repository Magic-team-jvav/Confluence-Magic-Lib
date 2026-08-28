package org.confluence.lib.client.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.confluence.lib.api.event.OnGatherEffectScreenTooltipsEvent;
import org.confluence.lib.client.DPSMeter;
import org.confluence.lib.client.LibKeyBindings;
import org.confluence.lib.client.animate.ExpertColorAnimation;
import org.confluence.lib.client.animate.MasterColorAnimation;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.integration.animation.AnimationConstants;
import org.confluence.lib.integration.animation.PlayerAttackingStatePacket;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.confluence.lib.util.LibClientUtils;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.client.PortGatherEffectScreenTooltipsEvent;

import java.util.List;
import java.util.Optional;

public final class LibClientGameEvents {
    public static void init() {
        PortEventHandler.addListener(LibClientGameEvents::clientTick);
        PortEventHandler.addListener(PortEventPriority.HIGHEST, LibClientGameEvents::renderTooltip);
        PortEventHandler.addListener(LibClientGameEvents::gatherEffectScreenTooltips);
        PortEventHandler.addListener(LibClientGameEvents::movementInputUpdate);
        PortEventHandler.addListener(LibClientGameEvents::playerTick$Pre);
        PortEventHandler.addListener(LibClientGameEvents::clientPlayerNetwork$LoggingOut);
        PortEventHandler.addListener(LibClientGameEvents::clientTick$Post);
        PortEventHandler.addListener(LibClientGameEvents::cameraSetup);
        PortEventHandler.addListener(LibClientGameEvents::input$InteractionKeyMappingTriggered);
    }

    private static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        ExpertColorAnimation.INSTANCE.updateColor();
        MasterColorAnimation.INSTANCE.updateColor();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            DPSMeter.checkDPSTime(player.level().getGameTime());
        }
    }

    private static void renderTooltip(RenderTooltipEvent.GatherComponents event) {
        ItemStack itemStack = event.getItemStack();
        if (itemStack.isEmpty()) return;

        List<Either<FormattedText, TooltipComponent>> tooltipElements = event.getTooltipElements();
        if (tooltipElements.isEmpty()) return;
        Optional<FormattedText> displayName = tooltipElements.get(0).left();
        if (displayName.isPresent() && displayName.get() instanceof MutableComponent component) {
            ModRarity rarity = ModRarity.getRarity(itemStack);
            if (rarity != null) {
                tooltipElements.set(0, Either.left(component.copy().withColor(rarity.color())));
            }
        }

        if (itemStack.is(LibTags.Items.WIP)) {
            event.getTooltipElements().add(1, Either.left(Component.translatable("tooltip.confluence.work_in_progress").withStyle(ChatFormatting.RED)));
        }
    }

    private static void gatherEffectScreenTooltips(PortGatherEffectScreenTooltipsEvent event) {
        MobEffect effect = event.getEffectInstance().getEffect();
        ResourceLocation id = ForgeRegistries.MOB_EFFECTS.getKey(effect);
        List<Component> tooltip = event.getTooltip();
        if (id != null) {
            String key = Util.makeDescriptionId("tooltip.effect", id) + ".0";
            if (!I18n.exists(key) && !PortEventHandler.postEventWithReturn(new OnGatherEffectScreenTooltipsEvent(effect, id, key, tooltip::add)).isCanceled()) {
                if (effect.equals(LibEffects.GRAVITATION.get())) {
                    tooltip.add(Component.translatable(key, LibClientUtils.keyMappingComponent(LibKeyBindings.FLIP_GRAVITATION.get())));
                } else {
                    tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
                }
            }
        }
        if (!ILibMobEffectInstance.of(event.getEffectInstance()).confluence$isEnabled()) {
            tooltip.add(Component.translatable("tooltip.confluence.disabled").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static void movementInputUpdate(MovementInputUpdateEvent event) {
        LocalPlayer player = (LocalPlayer) event.getEntity();
        MobEffectInstance effect = player.getEffect(LibEffects.GRAVITATION.get());
        if (effect != null) {
            if (effect.getAmplifier() > 0) {
                GravitationHandler.force(player);
            } else {
                GravitationHandler.handle(player);
            }
        } else if (GravitationHandler.isForceEnable()) {
            GravitationHandler.handle(player);
        } else {
            GravitationHandler.expire();
        }
    }

    private static void playerTick$Pre(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (event.player.isLocalPlayer()) {
            GravitationHandler.unCrouching(event.player);
        }
    }

    private static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GravitationHandler.reset();
    }

    private static void clientTick$Post(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            GravitationHandler.tryExpire(player);
        }
    }

    private static void cameraSetup(ViewportEvent.ComputeCameraAngles event) {
        if (GravitationHandler.isShouldRot()) {
            event.setRoll(180.0F);
        }
    }

    private static void input$InteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (AnimationConstants.SHOULD_APPLY && event.isAttack()) {
            PlayerAttackingStatePacket.sendToServer();
        }
    }
}
