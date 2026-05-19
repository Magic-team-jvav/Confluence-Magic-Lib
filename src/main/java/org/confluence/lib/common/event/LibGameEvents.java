package org.confluence.lib.common.event;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityInvulnerabilityCheckEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.event.PlayerNaturalHealEvent;
import org.confluence.lib.api.event.SwitchItemFunctionEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.data.saved.IGlobalData;
import org.confluence.lib.common.item.IFunctionCouldEnable;
import org.confluence.lib.mixed.ILibDamageSource;
import org.confluence.lib.mixed.ILibExtraSyncedData;
import org.confluence.lib.network.AttackDamagePacketS2C;
import org.confluence.lib.network.SetEntityDataPacketS2C;
import org.confluence.lib.util.DelayTaskHolder;
import org.confluence.lib.util.LibUtils;
import org.confluence.lib.util.NaturalSpawnerUtils;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class LibGameEvents {
    @SubscribeEvent
    public static void entityAttributeModification(EntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ConfluenceMagicLib.MOB_SPAWN_SPEED_MULTIPLIER);
        event.add(EntityType.PLAYER, ConfluenceMagicLib.MOB_SPAWN_COUNT_MULTIPLIER);
    }

    @SubscribeEvent
    public static void spawnClusterSize(SpawnClusterSizeEvent event) {
        Mob mob = event.getEntity();
        NaturalSpawnerUtils.ChunkSpawnData data = NaturalSpawnerUtils.getChunkSpawnData(mob.level().dimension(), mob.chunkPosition());
        if (data != NaturalSpawnerUtils.ChunkSpawnData.DEFAULT) {
            event.setSize(data.getCount(event.getSize()));
        }
    }

    @SubscribeEvent
    public static void livingDrops(LivingDropsEvent event) {
        if (event.getEntity().getTags().contains(LibUtils.NO_DROPS_TAG)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerNaturalHeal(PlayerNaturalHealEvent event) {
        if (event.getEntity().getActiveEffects().stream().anyMatch(instance -> instance.getCures().contains(LibUtils.DENY_HEAL))) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void player$StartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof ILibExtraSyncedData<?> extraSyncedData) {
            PacketDistributor.sendToPlayer(serverPlayer, new SetEntityDataPacketS2C(
                    extraSyncedData.confluence$self().getId(),
                    extraSyncedData.confluence$getAllEntries()
            ));
        }
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent event) {
        NaturalSpawnerUtils.init(event.getServer());
    }

    @SubscribeEvent
    public static void serverTick$Post(ServerTickEvent.Post event) {
        NaturalSpawnerUtils.update(event.getServer());
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent event) {
        NaturalSpawnerUtils.clear();
        IGlobalData.clearAll();
    }

    @SubscribeEvent
    public static void player$PlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        LibStartupConfig.checkIfSomeoneHasViolatedEULA(event.getEntity());
    }

    @SubscribeEvent
    public static void livingDeath(LivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        DelayTaskHolder delayTaskHolder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
        if (delayTaskHolder != null) {
            livingEntity.removeData(ConfluenceMagicLib.DELAY_TASK_HOLDER);
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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void entityTick$Pre(EntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity livingEntity) {
            if (livingEntity.isAlive()) {
                DelayTaskHolder delayTaskHolder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
                if (delayTaskHolder != null) {
                    delayTaskHolder.tick();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingEquipmentChange(LivingEquipmentChangeEvent event) {
        LivingEntity livingEntity = event.getEntity();
        EquipmentSlot slot = event.getSlot();
        if (livingEntity.isAlive()) {
            DelayTaskHolder delayTaskHolder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
            if (delayTaskHolder != null && !ItemStack.isSameItem(event.getFrom(), event.getTo())) {
                delayTaskHolder.removeTask(slot);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void livingSwapItems$Hands(LivingSwapItemsEvent.Hands event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.isAlive()) {
            DelayTaskHolder delayTaskHolder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
            if (delayTaskHolder != null) {
                ItemStack itemSwappedToMainHand = event.getItemSwappedToMainHand();
                ItemStack itemSwappedToOffHand = event.getItemSwappedToOffHand();
                if (!itemSwappedToMainHand.getItem().shouldCauseBlockBreakReset(itemSwappedToMainHand, itemSwappedToOffHand)) {
                    delayTaskHolder.removeTask(InteractionHand.MAIN_HAND);
                }
                if (!itemSwappedToOffHand.getItem().shouldCauseBlockBreakReset(itemSwappedToOffHand, itemSwappedToMainHand)) {
                    delayTaskHolder.removeTask(InteractionHand.OFF_HAND);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDamage$Post(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            AttackDamagePacketS2C.sendToClient(player, event.getNewDamage());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingDamage$Pre(LivingDamageEvent.Pre event) {
        float amount = event.getNewDamage();
        if (amount <= 0.0F) return; // 防止莫名的负数伤害
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource damageSource = event.getSource();
        if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        @Nullable Entity attacker = damageSource.getEntity();

        amount = LibAttributes.applyMagicDamage(attacker, damageSource, amount);
        amount = LibAttributes.applyRangedDamage(attacker, damageSource, amount);
        amount = ILibDamageSource.processCritical(attacker, amount, victim, damageSource);

        event.setNewDamage(amount);
    }

    @SubscribeEvent
    public static void entityInvulnerabilityCheck(EntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable()) return;
        DamageSource damageSource = event.getSource();
        if (damageSource.is(DamageTypes.FELL_OUT_OF_WORLD) || damageSource.is(DamageTypes.GENERIC_KILL)) {
            return;
        }
        if (event.getEntity() instanceof LivingEntity victim && LibAttributes.applyDodge(victim)) {
            event.setInvulnerable(true);
        }
    }

    @SubscribeEvent
    public static void entityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity living) {
            LibAttributes.applyToArrow(living, arrow);
        }
    }

    @SubscribeEvent
    public static void livingChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        if (!(self instanceof Enemy) || !(event.getNewAboutToBeSetTarget() instanceof Player playerO)) {
            return;
        }
        // 当自身为敌人且当新目标为玩家时
        double rangeSqr = Mth.square(self.getAttributeValue(Attributes.FOLLOW_RANGE));
        self.level().players().stream()
                .filter(player -> player.distanceToSqr(self) < rangeSqr && self.canAttack(player))
                .max((playerA, playerB) -> {
                    AttributeInstance instanceA = playerA.getAttribute(ConfluenceMagicLib.AGGRO);
                    AttributeInstance instanceB = playerB.getAttribute(ConfluenceMagicLib.AGGRO);
                    if (instanceA != null && instanceB != null) {
                        return Double.compare(instanceA.getValue(), instanceB.getValue());
                    }
                    return 0;
                }).ifPresent(player -> {
                    if (player == playerO) return;
                    AttributeInstance instanceO = playerO.getAttribute(ConfluenceMagicLib.AGGRO);
                    AttributeInstance instance = player.getAttribute(ConfluenceMagicLib.AGGRO);
                    if (instanceO != null && instance != null && instanceO.getValue() < instance.getValue()) {
                        event.setNewAboutToBeSetTarget(player); // 只有当新目标的仇恨值大于旧目标时，才设置新目标
                    }
                });
    }

    @SubscribeEvent
    public static void playerTick$Post(PlayerTickEvent.Post event) {
        LibAttributes.applyPickupRange(event.getEntity());
    }
}
