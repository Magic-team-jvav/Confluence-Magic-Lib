package org.confluence.lib.client.event;

import com.mojang.datafixers.util.Either;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.client.DPSMeter;
import org.confluence.lib.client.DynamicLightDispatcher;
import org.confluence.lib.client.color.ExpertColorAnimation;
import org.confluence.lib.client.color.MasterColorAnimation;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.common.LibTags;
import org.confluence.lib.common.component.ModRarity;
import org.confluence.lib.api.animation.third_person.AnimationConstants;
import org.confluence.lib.api.animation.third_person.PlayerAttackingStatePacket;
import org.confluence.lib.api.animation.third_person.PlayerGeoAnimatable;

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
    public static void input$InteractionKeyMappingTriggered(InputEvent.InteractionKeyMappingTriggered event) {
        if (AnimationConstants.SHOULD_APPLY && event.isAttack()) {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = Objects.requireNonNull(minecraft.player);
            PlayerAttackingStatePacket.sendToServer(player, minecraft.options.getCameraType().isFirstPerson());
        }
    }

    /// 重力反转的「输入」侧驱动（WP6c 第二步：整条特性从 1.21 的 TerraCurio 挪回 Lib，与 1.20 归属一致）。
    ///
    /// 1.20 原文：`Confluence-Magic-Lib` 的 `lib/client/event/LibClientGameEvents.java:106-120`，逐字。
    /// 唯一的 API 改写是第 2 行：1.20 写的是 `player.getEffect(LibEffects.GRAVITATION.get())`
    /// （那时 Forge 的 `getEffect` 收 `MobEffect`，`LibEffects.GRAVITATION` 是 RegistryObject），
    /// 1.21 的 `LivingEntity#getEffect` 收 **`Holder<MobEffect>`**（源码已核实：
    /// `LivingEntity.java` 的 `public MobEffectInstance getEffect(Holder<MobEffect> effect)`），
    /// 而 Lib 的 `LibEffects.GRAVITATION` 是 `DeferredHolder<MobEffect, MobEffect>`（**就是** Holder），
    /// 所以去掉 `.get()` —— 与 1.21 TerraCurio 的
    /// `terra_curio/client/event/GameClientEvents.java:61` 同一写法（那份是本条特性此前的宿主）。
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

    /// 1.20 原文：`LibClientGameEvents.java:122-127`。
    /// 1.20 用的是 Forge 的 `TickEvent.PlayerTickEvent` + `phase != START` 判断；
    /// 1.21 拆成了 `net.neoforged.neoforge.event.tick.PlayerTickEvent.{Pre,Post}` 两个事件类，
    /// 所以**去掉 phase 判断**、直接用 `.Pre`（本仓库 Lib 侧既有先例：
    /// `lib/common/event/LibGameEvents.java:253` 的 `playerTick$Post(PlayerTickEvent.Post event)`）。
    @SubscribeEvent
    public static void playerTick$Pre(PlayerTickEvent.Pre event) {
        if (event.getEntity().isLocalPlayer()) {
            GravitationHandler.unCrouching(event.getEntity());
        }
    }

    /// 1.20 原文：`LibClientGameEvents.java:138-144`（方法名 `clientTick$Post`，phase START 分支）。
    ///
    /// ⚠️ **方法名与 1.20 不同（有意）**：1.21 本文件里已有一个 `clientTick$Post(ClientTickEvent.Pre)`，
    /// 但它的**方法体其实是 1.20 的 `clientTick`**（`ExpertColorAnimation`/`MasterColorAnimation`/`DPSMeter`），
    /// 也就是说这个名字在 1.21 侧被用在了另一件事上。为避免重名（Java 不允许两个同签名方法），
    /// 这里按职责命名为 `clientTick$Gravitation`。
    /// 语义不变：1.20 的 `clientTick` 与 `clientTick$Post` 是**同一个事件（ClientTickEvent, phase START）
    /// 上的两个监听器**，二者互相独立（前者刷颜色动画、后者只做 `tryExpire`），先后顺序无语义影响。
    @SubscribeEvent
    public static void clientTick$Gravitation(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            GravitationHandler.tryExpire(player);
        }
    }

    /// 1.20 原文：`LibClientGameEvents.java:146-151`。
    ///
    /// ⚠️ **本方法刻意只搬了重力那半句**：1.20 的同一个处理器里还有一句
    /// `CameraAnimation.apply(event)`（第一人称镜头动画，与重力无关），
    /// 它在本仓库 1.21 侧**至今没有任何调用点**（全仓 grep：`CameraAnimation` 只出现在
    /// `lib/api/animation/first_person/CameraAnimation.java` 自身）。那是 G0 搬进来后
    /// 还没接线的东西，**不属于 WP6c 的重力特性**，为避免顺手改动无关行为（镜头动画会突然生效）
    /// 而没有一并加上 —— 需要时请另行确认后再接。
    ///
    /// 另一处与 1.21 TerraCurio 旧实现的差异：TerraCurio 写的是**绝对赋值** `setRoll(180.0F)`
    /// （`GameClientEvents.java:85`），而 1.20 Lib 是**叠加** `setRoll(getRoll() + 180)`。
    /// 这里按 1.20 写（本步的事实来源）：叠加式在「别的处理器（如镜头动画）已经改过 roll」时
    /// 才是正确的组合语义，绝对赋值会把它盖掉。
    @SubscribeEvent
    public static void viewport$ComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        if (GravitationHandler.isShouldRot()) {
            event.setRoll(event.getRoll() + 180);
        }
    }

    @SubscribeEvent
    public static void clientPlayerNetwork$LoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DynamicLightDispatcher.clearWorld();
        // 1.20 原文：`LibClientGameEvents.java:131` 的 `GravitationHandler.reset();`
        // （1.21 侧此前只有一行 `// todo 类似1.20.1 GravitationHandler.reset();`，本步落地它）。
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
