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
import net.minecraftforge.event.entity.player.PlayerEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.event.PlayerNaturalHealEvent;
import org.confluence.lib.api.event.SwitchItemFunctionEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.LibEffects;
import org.confluence.lib.common.data.saved.IGlobalData;
import org.confluence.lib.common.item.IFunctionCouldEnable;
import org.confluence.lib.mixed.ILibDamageSource;
import org.confluence.lib.mixed.ILibExtraSyncedData;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.confluence.lib.network.s2c.AttackDamagePacketS2C;
import org.confluence.lib.network.s2c.SetEntityDataPacketS2C;
import org.confluence.lib.util.DelayTaskHolder;
import org.confluence.lib.util.LibUtils;
import org.confluence.lib.util.NaturalSpawnerUtils;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.entity.PortEntityAttributeModificationEvent;
import org.mesdag.portlib.event.entity.PortEntityInvulnerabilityCheckEvent;
import org.mesdag.portlib.event.entity.PortEntityJoinLevelEvent;
import org.mesdag.portlib.event.entity.living.*;
import org.mesdag.portlib.event.other.PortItemStackedOnOtherEvent;
import org.mesdag.portlib.event.server.PortServerStartingEvent;
import org.mesdag.portlib.event.server.PortServerStoppedEvent;
import org.mesdag.portlib.event.tick.PortEntityTickEvent;
import org.mesdag.portlib.event.tick.PortPlayerTickEvent;
import org.mesdag.portlib.event.tick.PortServerTickEvent;

public final class LibGameEvents {
    public static void init() {
        PortEventHandler.addListener(LibGameEvents::entityAttributeModification);
        PortEventHandler.addListener(LibGameEvents::spawnClusterSize);
        PortEventHandler.addListener(LibGameEvents::livingDrops);
        PortEventHandler.addListener(LibGameEvents::playerNaturalHeal);
        PortEventHandler.addListener(LibGameEvents::playerStartTracking);
        PortEventHandler.addListener(LibGameEvents::serverStarting);
        PortEventHandler.addListener(LibGameEvents::serverTick);
        PortEventHandler.addListener(LibGameEvents::serverStopped);
        PortEventHandler.addListener(LibGameEvents::playerLoggedIn);
        PortEventHandler.addListener(LibGameEvents::livingDeath);
        PortEventHandler.addListener(PortEventPriority.HIGH, true, LibGameEvents::itemStackedOnOther);
        PortEventHandler.addListener(PortEventPriority.HIGHEST, LibGameEvents::entityTickPre);
        PortEventHandler.addListener(PortEventPriority.HIGHEST, LibGameEvents::livingEquipmentChange);
        PortEventHandler.addListener(PortEventPriority.HIGHEST, LibGameEvents::livingSwapItemsHands);
        PortEventHandler.addListener(PortEventPriority.LOWEST, LibGameEvents::livingDamage$Post);
        PortEventHandler.addListener(PortEventPriority.LOWEST, LibGameEvents::livingDamage$Pre);
        PortEventHandler.addListener(LibGameEvents::entityInvulnerabilityCheck);
        PortEventHandler.addListener(LibGameEvents::entityJoinLevel);
        PortEventHandler.addListener(LibGameEvents::livingChangeTarget);
        PortEventHandler.addListener(LibGameEvents::playerTick);
        PortEventHandler.addListener(LibGameEvents::effectParticleModification);
    }

    private static void entityAttributeModification(PortEntityAttributeModificationEvent event) {
        event.add(EntityType.PLAYER, ConfluenceMagicLib.MOB_SPAWN_SPEED_MULTIPLIER);
        event.add(EntityType.PLAYER, ConfluenceMagicLib.MOB_SPAWN_COUNT_MULTIPLIER);
    }

    private static void spawnClusterSize(PortSpawnClusterSizeEvent event) {
        Mob mob = event.getEntity();
        NaturalSpawnerUtils.ChunkSpawnData data = NaturalSpawnerUtils.getChunkSpawnData(mob.level().dimension(), mob.chunkPosition());
        if (data != NaturalSpawnerUtils.ChunkSpawnData.DEFAULT) {
            event.setSize(data.getCount(event.getSize()));
        }
    }

    private static void livingDrops(PortLivingDropsEvent event) {
        if (event.getEntity().getTags().contains(LibUtils.NO_DROPS_TAG)) event.setCanceled(true);
    }

    private static void playerNaturalHeal(PlayerNaturalHealEvent event) {
        if (event.getEntity().getActiveEffects().stream().anyMatch(instance -> instance.getCures().contains(LibUtils.DENY_HEAL))) {
            event.setCanceled(true);
        }
    }

    private static void playerStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer serverPlayer && event.getTarget() instanceof ILibExtraSyncedData<?> extraSyncedData) {
            ConfluenceMagicLib.NETWORK_HANDLER.sendToPlayer(serverPlayer, new SetEntityDataPacketS2C(extraSyncedData.confluence$self().getId(), extraSyncedData.confluence$getAllEntries()));
        }
    }

    private static void serverStarting(PortServerStartingEvent event) {NaturalSpawnerUtils.init(event.getServer());}

    private static void serverTick(PortServerTickEvent.Post event) {NaturalSpawnerUtils.update(event.getServer());}

    private static void serverStopped(PortServerStoppedEvent event) {
        NaturalSpawnerUtils.clear();
        IGlobalData.clearAll();
    }

    private static void playerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {LibStartupConfig.checkIfSomeoneHasViolatedEULA(event.getEntity());}

    private static void livingDeath(PortLivingDeathEvent event) {
        LivingEntity livingEntity = event.getEntity();
        DelayTaskHolder holder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
        if (holder != null) {
            livingEntity.removeData(ConfluenceMagicLib.DELAY_TASK_HOLDER);
        }
    }

    private static void itemStackedOnOther(PortItemStackedOnOtherEvent event) {
        if (event.getClickAction() != ClickAction.SECONDARY) return;
        ItemStack carried = event.getCarriedItem();
        ItemStack onSlot = event.getStackedOnItem();
        if (carried.isEmpty() && onSlot.getItem() instanceof IFunctionCouldEnable couldEnable) {
            Player player = event.getPlayer();
            if (!PortEventHandler.postEventWithReturn(new SwitchItemFunctionEvent.Pre(player, onSlot)).isCanceled()) {
                couldEnable.cycleEnable(onSlot);
                PortEventHandler.postEvent(new SwitchItemFunctionEvent.Post(player, onSlot, couldEnable.isEnabled(onSlot)));
                event.setCanceled(true);
            }
        }
    }

    private static void entityTickPre(PortEntityTickEvent.Pre event) {
        Entity entity = event.getEntity();
        if (entity instanceof LivingEntity livingEntity && livingEntity.isAlive()) {
            DelayTaskHolder holder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
            if (holder != null) holder.tick();
        }
    }

    private static void livingEquipmentChange(PortLivingEquipmentChangeEvent event) {
        LivingEntity livingEntity = event.getEntity();
        EquipmentSlot slot = event.getSlot();
        if (livingEntity.isAlive()) {
            DelayTaskHolder holder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
            if (holder != null && !ItemStack.isSameItem(event.getFrom(), event.getTo())) {
                holder.removeTask(slot);
            }
        }
    }

    private static void livingSwapItemsHands(PortLivingSwapItemsEvent.Hands event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.isAlive()) {
            DelayTaskHolder holder = livingEntity.getExistingDataOrNull(ConfluenceMagicLib.DELAY_TASK_HOLDER);
            if (holder != null) {
                ItemStack toMain = event.getItemSwappedToMainHand();
                ItemStack toOff = event.getItemSwappedToOffHand();
                if (!toMain.getItem().shouldCauseBlockBreakReset(toMain, toOff)) {
                    holder.removeTask(InteractionHand.MAIN_HAND);
                }
                if (!toOff.getItem().shouldCauseBlockBreakReset(toOff, toMain)) {
                    holder.removeTask(InteractionHand.OFF_HAND);
                }
            }
        }
    }

    private static void livingDamage$Post(PortLivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            AttackDamagePacketS2C.sendToClient(player, event.getNewDamage());
        }
    }

    private static void livingDamage$Pre(PortLivingDamageEvent.Pre event) {
        float amount = event.getNewDamage();
        if (amount <= 0.0F) return;
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        DamageSource damageSource = event.getSource();
        if (damageSource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        @Nullable Entity attacker = damageSource.getEntity();
        amount = LibAttributes.applyMagicDamage(attacker, damageSource, amount);
        amount = LibAttributes.applyRangedDamage(attacker, damageSource, amount);
        amount = ILibDamageSource.processCritical(attacker, amount, victim, damageSource);
        amount = LibEffects.applyPaladinsShield(victim, damageSource, amount);
        event.setNewDamage(amount);
    }

    private static void entityInvulnerabilityCheck(PortEntityInvulnerabilityCheckEvent event) {
        if (event.isInvulnerable()) return;
        DamageSource source = event.getSource();
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD) || source.is(DamageTypes.GENERIC_KILL)) return;
        if (event.getEntity() instanceof LivingEntity victim && LibAttributes.applyDodge(victim)) {
            event.setInvulnerable(true);
        }
    }

    private static void entityJoinLevel(PortEntityJoinLevelEvent event) {
        if (event.getEntity() instanceof AbstractArrow arrow && arrow.getOwner() instanceof LivingEntity living) {
            LibAttributes.applyToArrow(living, arrow);
        }
    }

    private static void livingChangeTarget(PortLivingChangeTargetEvent event) {
        LivingEntity self = event.getEntity();
        if (!(self instanceof Enemy) || !(event.getNewAboutToBeSetTarget() instanceof Player playerO))
            return;
        double rangeSqr = Mth.square(self.getAttributeValue(Attributes.FOLLOW_RANGE));
        self.level().players().stream()
                .filter(player -> player.distanceToSqr(self) < rangeSqr && self.canAttack(player))
                .max((a, b) -> {
                    AttributeInstance ia = a.getAttribute(ConfluenceMagicLib.AGGRO.get());
                    AttributeInstance ib = b.getAttribute(ConfluenceMagicLib.AGGRO.get());
                    return ia != null && ib != null ? Double.compare(ia.getValue(), ib.getValue()) : 0;
                }).ifPresent(player -> {
                    if (player == playerO) return;
                    AttributeInstance io = playerO.getAttribute(ConfluenceMagicLib.AGGRO.get());
                    AttributeInstance ip = player.getAttribute(ConfluenceMagicLib.AGGRO.get());
                    if (io != null && ip != null && io.getValue() < ip.getValue()) {
                        event.setNewAboutToBeSetTarget(player);
                    }
                });
    }

    private static void playerTick(PortPlayerTickEvent.Post event) {
        LibAttributes.applyPickupRange(event.getEntity());
    }

    private static void effectParticleModification(PortEffectParticleModificationEvent event) {
        if (event.isVisible() && !ILibMobEffectInstance.of(event.getEffect()).confluence$isEnabled()) {
            event.setVisible(false);
        }
    }
}
