package org.confluence.lib.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

public final class LibDamageTypes {
    public static final ResourceKey<DamageType> STAR_CLOAK = register("star_cloak");
    public static final ResourceKey<DamageType> GUN_BULLET = register("gun_bullet");
    /// 随 WP6 效果层批次补入（1.20 侧 `LibDamageTypes.java:21` + `:62`）。
    /// `message_id` 在 1.20 里就是 `frost_burn_damage_type`（不是 `frost_burn`），
    /// 对应的 `src/generated/resources/data/confluence_magic_lib/damage_type/frost_burn.json`
    /// 已从 1.20 逐字节拷入。
    public static final ResourceKey<DamageType> FROST_BURN = register("frost_burn");
    /// 随 WP2 批次 13 补入（1.20 侧 `LibDamageTypes.java:19` + `:60`，`DamageScaling.ALWAYS` / 0.1F）；
    /// 使用者是 `common/entity/boss/DungeonGuardian`（地牢守卫的接触伤害）。
    public static final ResourceKey<DamageType> DUNGEON_GUARDIAN = register("dungeon_guardian");
    public static final ResourceKey<DamageType> SUMMON = register("summon");
    /// 随坐骑簇的前置一起补入（1.20 侧 `LibDamageTypes.java` 的 `SUMMONER` 声明 + bootstrap
    /// `damageType(context, SUMMONER, "summoner_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F)`，
    /// 注意 `message_id` 是 `summoner_damage_type` 而不是 `summoner`）。
    /// 使用者是 `RideableUnicornMountEntity` / `RideableSlimeMountEntity` 的踩踏/冲撞伤害（见 `notes/WP2P-SUBSET.md` 第四节）。
    public static final ResourceKey<DamageType> SUMMONER = register("summoner");

    private static ResourceKey<DamageType> register(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, ConfluenceMagicLib.asResource(id));
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key) {
        return of(level, key, null, null);
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key, @Nullable Entity causing) {
        return of(level, key, causing, causing);
    }

    public static DamageSource of(Level level, ResourceKey<DamageType> key, @Nullable Entity causing, @Nullable Entity direct) {
        return level.damageSources().source(key, direct, causing);
    }

    public static void bootstrap(BootstrapContext<DamageType> context) {
        context.register(STAR_CLOAK, new DamageType("star_cloak", DamageScaling.ALWAYS, 5));
        context.register(GUN_BULLET, new DamageType("gun_bullet", DamageScaling.NEVER, 0.1F));
        context.register(FROST_BURN, new DamageType("frost_burn_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(DUNGEON_GUARDIAN, new DamageType("dungeon_guardian", DamageScaling.ALWAYS, 0.1F));
        context.register(SUMMON, new DamageType("summon_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
        context.register(SUMMONER, new DamageType("summoner_damage_type", DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER, 0.1F));
    }
}
