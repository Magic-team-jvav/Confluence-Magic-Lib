package org.confluence.lib.common.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.SpawnClusterSizeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.StartupConfig;
import org.confluence.lib.common.data.IdFixer;
import org.confluence.lib.common.data.saved.IGlobalData;
import org.confluence.lib.common.item.IFunctionCouldEnable;
import org.confluence.lib.event.SwitchItemFunctionEvent;
import org.confluence.lib.mixed.IExtraSyncedData;
import org.confluence.lib.network.SetEntityDataPacketS2C;
import org.confluence.lib.util.LibUtils;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class GameEvents {
    @SubscribeEvent
    public static void addAttribute(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ConfluenceMagicLib.ENEMY_SPAWN_SPEED_MULTIPLIER);
        event.add(EntityType.PLAYER, ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER);
    }

    // TODO 缓存玩家信息
    // TODO 限制刷新BlockBehaviour为 isPersistent false
    @SubscribeEvent
    public static void onSpawnClusterSize(SpawnClusterSizeEvent event) {
        var entity = event.getEntity();
        Level level = entity.level();
        Vec3 position = entity.position();
        double x = position.x;
        double y = position.y;
        double z = position.z;
        var player = level.getNearestPlayer(x, y, z, -1, false);
        if (player == null ||
                !player.getAttributes().hasAttribute(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER) ||
                player.distanceToSqr(x, y, z) <= 576.0) {
            return;
        }
        event.setSize(Mth.ceil(event.getSize() * player.getAttributeValue(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER)));
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(LibUtils.NO_DROPS_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingHeal(LivingHealEvent event) {
        if (event.getEntity().level().isClientSide) return;
        if (event.getEntity().getActiveEffects().stream().anyMatch(instance -> instance.getCures().contains(LibUtils.DENY_HEAL))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof IExtraSyncedData<?> extraSyncedData) {
            PacketDistributor.sendToPlayer(serverPlayer, new SetEntityDataPacketS2C(
                    extraSyncedData.confluence$self().getId(),
                    extraSyncedData.confluence$getAllEntries()
            ));
        }
    }

    @SubscribeEvent
    public static void serverStop(ServerStoppedEvent event) {
        for (IGlobalData data : IGlobalData.DAT) {
            data.clear();
        }
    }

    @SubscribeEvent
    public static void playerLogged(PlayerEvent.PlayerLoggedInEvent event) {
        IdFixer.fixPersistentData(event.getEntity());
        StartupConfig.checkIfSomeoneHasViolatedEULA(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void checkForNull(LivingDeathEvent event) {
        if (event.getSource() == null) {
            event.setCanceled(true);
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                StringBuilder builder = new StringBuilder();
                for (StackTraceElement element : server.getRunningThread().getStackTrace()) {
                    builder.append(element).append('\n');
                }
                ConfluenceMagicLib.LOGGER.error(builder.toString());
                server.sendSystemMessage(Component.translatable("error.confluence.null").withStyle(ChatFormatting.RED));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH, receiveCanceled = true)
    public static void itemStackedOnOther(ItemStackedOnOtherEvent event) {
        if (event.getClickAction() != ClickAction.SECONDARY) return;
        ItemStack carried = event.getCarriedItem();
        ItemStack onSlot = event.getStackedOnItem();
        // 需要注意创造模式物品栏是仅客户端的，所以创造模式无法正常使用
        if (carried.isEmpty() && onSlot.getItem() instanceof IFunctionCouldEnable couldEnable) {
            Player player = event.getPlayer();
            if (!NeoForge.EVENT_BUS.post(new SwitchItemFunctionEvent.Pre(player, onSlot)).isCanceled()) {
                couldEnable.cycleEnable(onSlot);
                NeoForge.EVENT_BUS.post(new SwitchItemFunctionEvent.Post(player, onSlot, couldEnable.isEnabled(onSlot)));
                event.setCanceled(true);
            }
        }
    }
}
