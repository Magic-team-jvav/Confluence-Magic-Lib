package org.confluence.lib.client.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.api.animation.third_person.AnimationConstants;
import org.confluence.lib.api.animation.third_person.PlayerAttackingStatePacket;
import org.confluence.lib.api.animation.third_person.PlayerGeoAnimatable;
import org.confluence.lib.api.event.OnGatherEffectScreenTooltipsEvent;
import org.confluence.lib.client.DPSMeter;
import org.confluence.lib.client.DynamicLightDispatcher;
import org.confluence.lib.client.LibKeyBindings;
import org.confluence.lib.client.color.ExpertColorAnimation;
import org.confluence.lib.client.color.MasterColorAnimation;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.confluence.lib.util.LibClientUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)
public final class LibClientGameEvents {
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
    public static void gatherEffectScreenTooltips(GatherEffectScreenTooltipsEvent event) {
        Holder<MobEffect> effect = event.getEffectInstance().getEffect();
        Optional<ResourceKey<MobEffect>> optional = effect.unwrapKey();
        List<Component> tooltip = event.getTooltip();
        if (optional.isPresent()) {
            ResourceLocation id = optional.get().location();
            String key = Util.makeDescriptionId("tooltip.effect", id) + ".0";
            if (!I18n.exists(key) && !NeoForge.EVENT_BUS.post(new OnGatherEffectScreenTooltipsEvent(effect, id, key, tooltip::add)).isCanceled()) {
                if (effect.equals(LibEffects.GRAVITATION)) {
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

    @SubscribeEvent
    public static void input$InteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (AnimationConstants.SHOULD_APPLY && event.isAttack()) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = Objects.requireNonNull(minecraft.player);
            PlayerAttackingStatePacket.sendToServer(player, minecraft.options.getCameraType().isFirstPerson());
        }
    }

    ///
    @SubscribeEvent
    public static void movementInputUpdate(MovementInputUpdateEvent event) {
        LocalPlayer player = (LocalPlayer) event.getEntity();
        MobEffectInstance effect = player.getEffect(LibEffects.GRAVITATION);
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

    @SubscribeEvent
    public static void playerTick$Pre(PlayerTickEvent.Pre event) {
        if (event.getEntity().isLocalPlayer()) {
            GravitationHandler.unCrouching(event.getEntity());
        }
    }

    @SubscribeEvent
    public static void clientTick$Gravitation(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            GravitationHandler.tryExpire(player);
        }
    }

    @SubscribeEvent
    public static void viewport$ComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (GravitationHandler.isShouldRot()) {
            event.setRoll(event.getRoll() + 180);
        }
    }

    @SubscribeEvent
    public static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DynamicLightDispatcher.clearWorld();
        GravitationHandler.reset();
        if (AnimationConstants.SHOULD_APPLY) {
            PlayerGeoAnimatable.reloadCallbacks.clear();
        }
    }

    @SubscribeEvent
    public static void renderLevelStage$DynamicLight(RenderLevelStageEvent event) {
        DynamicLightDispatcher.update(event);
    }
}
