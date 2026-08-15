package org.confluence.lib.api.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.util.LibMathUtils;
import org.mesdag.portlib.registries.PortRegistryEntry;

import java.util.Objects;

/**
 * 将发射者当前属性解析为不可变弹幕战斗快照。
 *
 * <p>实体解析入口只能在逻辑服务端主线程调用。纯公式入口不访问世界，供附属模组、
 * 数据校验与自动化测试复用。</p>
 */
public final class ProjectileAttributeResolver {
    private ProjectileAttributeResolver() {}

    /**
     * 按唯一主通道解析伤害，禁止同时叠加远程与魔法倍率。
     */
    public static float resolveDamage(
            ProjectileDamageChannel channel,
            float baseDamage,
            float rangedMultiplier,
            float magicMultiplier
    ) {
        return resolveDamage(
                channel,
                baseDamage,
                rangedMultiplier,
                magicMultiplier,
                1.0F
        );
    }

    /**
     * 按唯一主通道解析伤害，召唤攻击不会借用远程或魔法倍率。
     *
     * <p>四参数重载继续服务已经接入的武器；需要召唤通道的调用方应显式传入召唤倍率。</p>
     */
    public static float resolveDamage(
            ProjectileDamageChannel channel,
            float baseDamage,
            float rangedMultiplier,
            float magicMultiplier,
            float summonMultiplier
    ) {
        Objects.requireNonNull(channel, "Damage channel must not be null");
        requireNonNegative(baseDamage, "Base damage");
        requireNonNegative(rangedMultiplier, "Ranged damage multiplier");
        requireNonNegative(magicMultiplier, "Magic damage multiplier");
        requireNonNegative(summonMultiplier, "Summon damage multiplier");
        float multiplier = switch (channel) {
            case MELEE -> 1.0F;
            case RANGED -> rangedMultiplier;
            case MAGIC -> magicMultiplier;
            case SUMMON -> summonMultiplier;
        };
        float result = baseDamage * multiplier;
        if (!Float.isFinite(result)) {
            throw new IllegalArgumentException("Resolved damage must be finite");
        }
        return result;
    }

    /**
     * 只有远程通道应用远程弹速属性。
     */
    public static float resolveVelocity(
            ProjectileDamageChannel channel,
            float baseVelocity,
            float rangedVelocityMultiplier
    ) {
        Objects.requireNonNull(channel, "Damage channel must not be null");
        requirePositive(baseVelocity, "Base velocity");
        requireNonNegative(rangedVelocityMultiplier, "Ranged velocity multiplier");
        float result = channel == ProjectileDamageChannel.RANGED
                ? baseVelocity * rangedVelocityMultiplier
                : baseVelocity;
        return requirePositive(result, "Resolved velocity");
    }

    /**
     * 按攻击击退属性解析最终弹幕击退。
     */
    public static float resolveKnockback(float baseKnockback, double attackKnockback) {
        requireNonNegative(baseKnockback, "Base knockback");
        if (!Double.isFinite(attackKnockback) || attackKnockback < 0.0) {
            throw new IllegalArgumentException("Attack knockback must be finite and non-negative");
        }
        double result = baseKnockback * (1.0 + attackKnockback);
        if (!Double.isFinite(result)) {
            throw new IllegalArgumentException("Resolved knockback must be finite");
        }
        return (float) result;
    }

    /**
     * 解析发射时暴击三态。固有暴击优先；外部属性接管时不由 MagicLib 抽取。
     */
    public static CriticalResolution resolveCritical(
            boolean inherentCritical,
            boolean externallyManaged,
            double criticalChance,
            RandomSource random
    ) {
        return resolveCritical(
                inherentCritical, externallyManaged, criticalChance, 0.0F, random);
    }

    /**
     * 把玩家属性与动作额外暴击率合并为一次发射时裁定。
     *
     * <p>外部系统接管玩家属性时，MagicLib 仍会裁定动作自身的额外概率；若动作未暴击则返回
     * {@link CriticalResolution#UNRESOLVED}，由命中阶段的扩展事件继续处理外部属性。</p>
     */
    public static CriticalResolution resolveCritical(
            boolean inherentCritical,
            boolean externallyManaged,
            double criticalChance,
            float criticalChanceBonus,
            RandomSource random
    ) {
        Objects.requireNonNull(random, "Critical random source must not be null");
        if (!Double.isFinite(criticalChance) || criticalChance < 0.0) {
            throw new IllegalArgumentException("Critical chance must be finite and non-negative");
        }
        requireNonNegative(criticalChanceBonus, "Critical chance bonus");
        if (inherentCritical) return CriticalResolution.CRITICAL;
        if (externallyManaged) {
            if (criticalChanceBonus <= 0.0F) {
                return CriticalResolution.UNRESOLVED;
            }
            return LibMathUtils.checkChance(criticalChanceBonus, random)
                    ? CriticalResolution.CRITICAL
                    : CriticalResolution.UNRESOLVED;
        }
        double totalChance = criticalChance + criticalChanceBonus;
        if (!Double.isFinite(totalChance)) {
            throw new IllegalArgumentException("Total critical chance must be finite");
        }
        return LibMathUtils.checkChance(totalChance, random)
                ? CriticalResolution.CRITICAL
                : CriticalResolution.NON_CRITICAL;
    }

    /**
     * 从服务端发射者当前属性构建完整快照。
     *
     * @param shooter          服务端发射者
     * @param weapon           发射瞬间使用的武器
     * @param channel          唯一主伤害通道
     * @param baseDamage       动作已解析的基础伤害
     * @param baseVelocity     动作基础速度
     * @param baseKnockback    动作基础击退
     * @param inherentCritical 原版或具体武器已经确定的固有暴击
     */
    public static ProjectileCombatSnapshot resolve(
            LivingEntity shooter,
            ItemStack weapon,
            ProjectileDamageChannel channel,
            float baseDamage,
            float baseVelocity,
            float baseKnockback,
            boolean inherentCritical
    ) {
        return resolve(
                shooter, weapon, channel, baseDamage, baseVelocity, baseKnockback,
                inherentCritical, 0.0F);
    }

    /**
     * 从服务端发射者当前属性和请求局部暴击率构建完整快照。
     *
     * @param criticalChanceBonus 动作本身提供、需要与玩家属性统一裁定的额外暴击率
     */
    public static ProjectileCombatSnapshot resolve(
            LivingEntity shooter,
            ItemStack weapon,
            ProjectileDamageChannel channel,
            float baseDamage,
            float baseVelocity,
            float baseKnockback,
            boolean inherentCritical,
            float criticalChanceBonus
    ) {
        Objects.requireNonNull(shooter, "Shooter must not be null");
        Objects.requireNonNull(weapon, "Weapon must not be null");
        Objects.requireNonNull(channel, "Damage channel must not be null");
        if (!(shooter.level() instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("Projectile combat snapshots can only be resolved on the logical server");
        }
        if (!serverLevel.getServer().isSameThread()) {
            throw new IllegalStateException("Projectile combat snapshots must be resolved on the server thread");
        }

        boolean rangedDamageExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.RANGED_DAMAGE);
        boolean magicDamageExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.MAGIC_DAMAGE);
        boolean summonDamageExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.SUMMON_DAMAGE);
        boolean rangedVelocityExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.RANGED_VELOCITY);
        boolean summonKnockbackExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.SUMMON_KNOCKBACK);
        boolean criticalExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.CRITICAL_CHANCE);
        boolean armorPenetrationExternal = LibAttributes.hasCustomAttribute(ConfluenceMagicLib.ARMOR_PENETRATION);

        float rangedMultiplier = rangedDamageExternal
                ? 1.0F
                : (float) attributeValueOrDefault(shooter, ConfluenceMagicLib.RANGED_DAMAGE);
        float magicMultiplier = magicDamageExternal
                ? 1.0F
                : (float) attributeValueOrDefault(shooter, ConfluenceMagicLib.MAGIC_DAMAGE);
        float summonMultiplier = summonDamageExternal
                ? 1.0F
                : (float) attributeValueOrDefault(shooter, ConfluenceMagicLib.SUMMON_DAMAGE);
        float rangedVelocityMultiplier = rangedVelocityExternal
                ? 1.0F
                : (float) attributeValueOrDefault(shooter, ConfluenceMagicLib.RANGED_VELOCITY);
        float channelMultiplier = switch (channel) {
            case MELEE -> 1.0F;
            case RANGED -> rangedMultiplier;
            case MAGIC -> magicMultiplier;
            case SUMMON -> summonMultiplier;
        };

        // 即使最终伤害要在原有受伤事件阶段应用，也先验证乘法不会溢出。
        resolveDamage(channel, baseDamage, rangedMultiplier, magicMultiplier, summonMultiplier);
        float velocity = resolveVelocity(channel, baseVelocity, rangedVelocityMultiplier);
        float knockback;
        if (channel == ProjectileDamageChannel.SUMMON) {
            double summonKnockback = summonKnockbackExternal
                    ? 0.0
                    : attributeValueOrDefault(shooter, ConfluenceMagicLib.SUMMON_KNOCKBACK);
            knockback = requireNonNegative(
                    (float) (baseKnockback + summonKnockback), "Resolved summon knockback");
        } else {
            knockback = resolveKnockback(
                    baseKnockback,
                    shooter.getAttributeValue(Attributes.ATTACK_KNOCKBACK)
            );
        }
        float armorPenetration = armorPenetrationExternal
                ? 0.0F
                : requireNonNegative((float) attributeValueOrDefault(shooter, ConfluenceMagicLib.ARMOR_PENETRATION),
                "Armor penetration");
        double criticalChance = criticalExternal
                ? 0.0
                : attributeValueOrDefault(shooter, ConfluenceMagicLib.CRITICAL_CHANCE);
        CriticalResolution critical = resolveCritical(
                inherentCritical,
                criticalExternal,
                criticalChance,
                criticalChanceBonus,
                shooter.getRandom());
        return new ProjectileCombatSnapshot(
                weapon,
                channel,
                baseDamage,
                channelMultiplier,
                velocity,
                knockback,
                armorPenetration,
                critical
        );
    }

    /**
     * 读取发射者属性；如果实体没有对应实例，则使用属性注册时声明的默认值。
     *
     * <p>原版 {@link LivingEntity#getAttributeValue(Attribute)} 在实例缺失时返回 {@code 0}。这对暴击率、
     * 护甲穿透等默认零值属性没有问题，但会把魔法伤害、远程伤害、召唤伤害和远程弹速这些默认一倍
     * 的倍率错误冻结成零，导致已经命中的弹幕在伤害事件链中被丢弃。</p>
     */
    private static double attributeValueOrDefault(
            LivingEntity shooter,
            PortRegistryEntry<Attribute, ? extends Attribute> attribute
    ) {
        AttributeInstance instance = shooter.getAttribute(attribute.get());
        return instance == null ? attribute.get().getDefaultValue() : instance.getValue();
    }

    private static float requireNonNegative(float value, String fieldName) {
        if (!Float.isFinite(value) || value < 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and non-negative");
        }
        return value;
    }

    private static float requirePositive(float value, String fieldName) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(fieldName + " must be finite and positive");
        }
        return value;
    }
}
