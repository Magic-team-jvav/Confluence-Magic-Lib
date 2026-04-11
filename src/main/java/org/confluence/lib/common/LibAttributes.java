package org.confluence.lib.common;

import it.unimi.dsi.fastutil.objects.ObjectCollection;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import net.minecraft.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.event.ArmorPenetrationEvent;
import org.confluence.lib.event.CustomPickupRangeEvent;
import org.confluence.lib.integration.apothic.ApothicHelper;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public final class LibAttributes {
    private static final Map<Holder<Attribute>, Holder<Attribute>> MAP = Util.make(new HashMap<>(), table -> {
        table.put(ConfluenceMagicLib.CRITICAL_CHANCE, null);
        table.put(ConfluenceMagicLib.RANGED_DAMAGE, null);
        table.put(ConfluenceMagicLib.RANGED_VELOCITY, null);
        table.put(ConfluenceMagicLib.DODGE_CHANCE, null);
        table.put(ConfluenceMagicLib.MAGIC_DAMAGE, null);
        table.put(ConfluenceMagicLib.ARMOR_PENETRATION, null);
    });

    @ApiStatus.Internal
    public static void registerAttribute(Holder<Attribute> attribute, BiConsumer<EntityType<? extends LivingEntity>, Holder<Attribute>> consumer) {
        if (!hasCustomAttribute(attribute)) consumer.accept(EntityType.PLAYER, attribute);
    }

    @ApiStatus.Internal
    public static void prepareReplacements() {
        Map<String, Holder<Attribute>> available = Map.of(
                "crit_chance", ConfluenceMagicLib.CRITICAL_CHANCE,
                "ranged_velocity", ConfluenceMagicLib.RANGED_VELOCITY,
                "ranged_damage", ConfluenceMagicLib.RANGED_DAMAGE,
                "dodge_chance", ConfluenceMagicLib.DODGE_CHANCE,
                "magic_damage", ConfluenceMagicLib.MAGIC_DAMAGE,
                "armor_penetration", ConfluenceMagicLib.ARMOR_PENETRATION
        );

        ApothicHelper.preset(MAP);

        List<? extends String> attributes = LibStartupConfig.ATTRIBUTE_REPLACE.get();
        for (String attribute : attributes) {
            String[] split = attribute.split("=");
            if (split.length != 2) {
                ConfluenceMagicLib.LOGGER.warn("Bad format of '{}', which must contains exactly one '='", attribute);
                continue;
            }
            Holder<Attribute> holder = available.get(split[0].strip());
            if (holder == null) {
                ConfluenceMagicLib.LOGGER.warn("Unsupported attribute: {}", split[0].strip());
                continue;
            }
            Optional<Holder.Reference<Attribute>> optional = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(split[1].strip()));
            if (optional.isEmpty()) {
                ConfluenceMagicLib.LOGGER.warn("Unknown attribute: {}", split[1].strip());
            } else {
                MAP.replace(holder, optional.get());
            }
        }
    }

    @ApiStatus.Internal
    public static void applyToArrow(LivingEntity living, AbstractArrow abstractArrow) {
        AttributeInstance instance;
        if (!hasCustomAttribute(ConfluenceMagicLib.RANGED_VELOCITY)) {
            instance = living.getAttribute(ConfluenceMagicLib.RANGED_VELOCITY);
            if (instance != null) {
                abstractArrow.setDeltaMovement(abstractArrow.getDeltaMovement().scale(instance.getValue()));
            }
        }
        if (!abstractArrow.isCritArrow() && !hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE)) {
            instance = living.getAttribute(ConfluenceMagicLib.CRITICAL_CHANCE);
            if (instance != null) {
                abstractArrow.setCritArrow(LibMathUtils.checkChance(instance.getValue(), living.getRandom()));
            }
        }
    }

    @ApiStatus.Internal
    public static double applyArrowKnockback(Entity attacker, double original) {
        if (attacker instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(Attributes.ATTACK_KNOCKBACK);
            if (instance != null) {
                original *= (1.0 + instance.getValue());
            }
        }
        return original;
    }

    @ApiStatus.Internal
    public static boolean applyDodge(LivingEntity victim) {
        if (hasCustomAttribute(ConfluenceMagicLib.DODGE_CHANCE)) return false;
        AttributeInstance instance = victim.getAttribute(ConfluenceMagicLib.DODGE_CHANCE);
        if (instance == null) return false;
        return LibMathUtils.checkChance(instance.getValue(), victim.getRandom());
    }

    @ApiStatus.Internal
    public static float applyRangedDamage(@Nullable Entity attacker, DamageSource damageSource, float amount) {
        if (attacker instanceof LivingEntity living &&
                damageSource.is(DamageTypeTags.IS_PROJECTILE) &&
                hasCustomAttribute(ConfluenceMagicLib.RANGED_DAMAGE)
        ) {
            AttributeInstance instance = living.getAttribute(ConfluenceMagicLib.RANGED_DAMAGE);
            if (instance != null) {
                amount *= (float) instance.getValue();
            }
        }
        return amount;
    }

    @ApiStatus.Internal
    public static float applyMagicDamage(@Nullable Entity attacker, DamageSource damageSource, float amount) {
        if (attacker instanceof LivingEntity living &&
                damageSource.is(Tags.DamageTypes.IS_MAGIC) &&
                hasCustomAttribute(ConfluenceMagicLib.MAGIC_DAMAGE)
        ) {
            AttributeInstance instance = living.getAttribute(ConfluenceMagicLib.MAGIC_DAMAGE);
            if (instance != null) {
                amount *= (float) instance.getValue();
            }
        }
        return amount;
    }

    @ApiStatus.Internal
    public static float applyArmorPenetration(LivingEntity victim, DamageSource damageSource, float armorValue) {
        if (damageSource.getEntity() instanceof LivingEntity attacker) {
            if (!hasCustomAttribute(ConfluenceMagicLib.ARMOR_PENETRATION)) {
                AttributeInstance instance = attacker.getAttribute(ConfluenceMagicLib.ARMOR_PENETRATION);
                if (instance != null) armorValue -= (float) instance.getValue();
            }
            float penetration = NeoForge.EVENT_BUS.post(new ArmorPenetrationEvent(victim, damageSource, armorValue)).getPenetration();
            return Math.max(armorValue - penetration, 0.0F);
        }
        return armorValue;
    }

    @ApiStatus.Internal
    public static void applyPickupRange(Player player) {
        CustomPickupRangeEvent event = new CustomPickupRangeEvent(player);
        NeoForge.EVENT_BUS.post(event);
        if (event.getRanges() == null) return;
        ObjectCollection<ObjectDoublePair<Predicate<ItemStack>>> values = event.getRanges().values();
        double maxRange = values.stream().mapToDouble(ObjectDoublePair::rightDouble).max().orElse(0.0);
        if (maxRange <= 0.0) return;
        for (ItemEntity entity : player.level().getEntitiesOfClass(
                ItemEntity.class,
                new AABB(player.blockPosition()).inflate(maxRange),
                entity -> !entity.hasPickUpDelay()
        )) {
            if (entity.isRemoved()) return;
            ItemStack stack = entity.getItem();
            double sqr = entity.position().distanceToSqr(player.position());
            if (values.stream().anyMatch(pair -> sqr > pair.rightDouble() && pair.left().test(stack))) {
                return;
            }

            entity.addDeltaMovement(player.position().subtract(entity.getX(), entity.getY(), entity.getZ()).normalize().scale(0.05F).add(0, 0.04F, 0));
            entity.move(MoverType.SELF, entity.getDeltaMovement());
        }
    }

    public static Holder<Attribute> getCustomAttribute(Holder<Attribute> attribute) {
        Holder<Attribute> target = MAP.get(attribute);
        if (target == null) return attribute;
        return target;
    }

    public static boolean hasCustomAttribute(Holder<Attribute> attribute) {
        Holder<Attribute> holder = MAP.get(attribute);
        return holder != null && !holder.equals(attribute);
    }

    public static Holder<Attribute> getCriticalChance() {
        return getCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE);
    }

    public static Holder<Attribute> getRangedVelocity() {
        return getCustomAttribute(ConfluenceMagicLib.RANGED_VELOCITY);
    }

    public static Holder<Attribute> getDodgeChance() {
        return getCustomAttribute(ConfluenceMagicLib.DODGE_CHANCE);
    }

    public static Holder<Attribute> getArmorPenetration() {
        return getCustomAttribute(ConfluenceMagicLib.ARMOR_PENETRATION);
    }

    /// 近战伤害
    public static Holder<Attribute> getAttackDamage() {
        return Attributes.ATTACK_DAMAGE;
    }

    /// 远程伤害
    public static Holder<Attribute> getRangedDamage() {
        return getCustomAttribute(ConfluenceMagicLib.RANGED_DAMAGE);
    }

    /// 魔法伤害
    public static Holder<Attribute> getMagicDamage() {
        return getCustomAttribute(ConfluenceMagicLib.MAGIC_DAMAGE);
    }

    /// 召唤伤害
    public static Holder<Attribute> getSummonDamage() {
        return ConfluenceMagicLib.SUMMON_DAMAGE;
    }
}
