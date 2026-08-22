package org.confluence.lib.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

/// 通用伤害类型常量，供所有子模块使用
public final class LibDamageTypes {
    public static final ResourceKey<DamageType> ACID_VENOM = register("acid_venom");
    public static final ResourceKey<DamageType> BOULDER = register("boulder");
    public static final ResourceKey<DamageType> CURSED_INFERNO = register("cursed_inferno");
    public static final ResourceKey<DamageType> DARKNESS = register("darkness");
    public static final ResourceKey<DamageType> DUNGEON_GUARDIAN = register("dungeon_guardian");
    public static final ResourceKey<DamageType> FALLING_STAR = register("falling_star");
    public static final ResourceKey<DamageType> FROST_BURN = register("frost_burn");
    public static final ResourceKey<DamageType> GUN_BULLET = register("gun_bullet");
    public static final ResourceKey<DamageType> HELLFIRE = register("hellfire");
    public static final ResourceKey<DamageType> MAGICAL_PROJECTILE = register("magical_projectile");
    public static final ResourceKey<DamageType> SPEAR_PROJECTILE = register("spear_projectile");
    public static final ResourceKey<DamageType> STAR_CLOAK = register("star_cloak");
    public static final ResourceKey<DamageType> SUMMON = register("summon");
    public static final ResourceKey<DamageType> SUMMONER = register("summoner");
    public static final ResourceKey<DamageType> SWORD_PROJECTILE = register("sword_projectile");

    private static ResourceKey<DamageType> register(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ConfluenceMagicLib.asResource(id));
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key) {
        return of(level, key, null, null);
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key, @Nullable Entity causing) {
        return of(level, key, causing, causing);
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key, @Nullable Entity direct, @Nullable Entity causing) {
        return level.damageSources().source(key, direct, causing);
    }

    /// 1.20 没有 1.21 的 `minecraft:no_knockback` 伤害类型标签，结算后恢复原速度以保持等效行为。
    public static boolean hurtWithoutKnockback(Entity target, DamageSource source, float amount) {
        Vec3 movement = target.getDeltaMovement();
        boolean hurt = target.hurt(source, amount);
        target.setDeltaMovement(movement);
        return hurt;
    }

    public static void bootstrap(BootstapContext<DamageType> context) {
        damageType(context, ACID_VENOM, "acid_venom", DamageScaling.ALWAYS, 10);
        damageType(context, BOULDER, "boulder", DamageScaling.ALWAYS, 5);
        damageType(context, CURSED_INFERNO, "cursed_inferno", DamageScaling.ALWAYS, 10, DamageEffects.BURNING);
        damageType(context, DARKNESS, "darkness", DamageScaling.ALWAYS, 20);
        damageType(context, DUNGEON_GUARDIAN, "dungeon_guardian", DamageScaling.ALWAYS, 0.1F);
        damageType(context, FALLING_STAR, "falling_star", DamageScaling.ALWAYS, 10);
        damageType(context, FROST_BURN, "frost_burn_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, GUN_BULLET, "gun_bullet", DamageScaling.NEVER, 0.1F);
        damageType(context, HELLFIRE, "hellfire_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, MAGICAL_PROJECTILE, "magical_projectile", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, SPEAR_PROJECTILE, "spear_projectile", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, STAR_CLOAK, "star_cloak", DamageScaling.ALWAYS, 5);
        damageType(context, SUMMON, "summon_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, SUMMONER, "summoner_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
        damageType(context, SWORD_PROJECTILE, "sword_projectile", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F);
    }

    private static void damageType(BootstapContext<DamageType> context, ResourceKey<DamageType> key, String messageId, DamageScaling scaling, float exhaustion, DamageEffects effects, DeathMessageType deathMessageType) {
        context.register(key, new DamageType(messageId, scaling, exhaustion, effects, deathMessageType));
    }

    private static void damageType(BootstapContext<DamageType> context, ResourceKey<DamageType> key, String messageId, DamageScaling scaling, float exhaustion, DamageEffects effects) {
        damageType(context, key, messageId, scaling, exhaustion, effects, DeathMessageType.DEFAULT);
    }

    private static void damageType(BootstapContext<DamageType> context, ResourceKey<DamageType> key, String messageId, DamageScaling scaling, float exhaustion) {
        damageType(context, key, messageId, scaling, exhaustion, DamageEffects.HURT, DeathMessageType.DEFAULT);
    }
}
