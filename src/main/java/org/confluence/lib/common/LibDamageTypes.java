package org.confluence.lib.common;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

/**
 * 通用伤害类型常量，供所有子模块使用
 */
public final class LibDamageTypes {
    // 枪械伤害（无无敌帧）
    public static final ResourceKey<DamageType> BULLET_DAMAGE = register("bullet_damage");
    // 霜冻伤害
    public static final ResourceKey<DamageType> FROST_BURN = register("frost_burn");
    // 狱炎伤害
    public static final ResourceKey<DamageType> HELLFIRE = register("hellfire");

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

    public static void bootstrap(BootstapContext<DamageType> context) {
        context.register(BULLET_DAMAGE, new DamageType("bullet_damage", DamageScaling.NEVER, 0.1F));
        context.register(FROST_BURN, new DamageType("frost_burn_damage_type", 0.1F));
        context.register(HELLFIRE, new DamageType("hellfire_damage_type", 0.1F));
    }
}
