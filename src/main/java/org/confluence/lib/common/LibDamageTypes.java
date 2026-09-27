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
    }
}
