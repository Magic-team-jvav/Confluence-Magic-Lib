package org.confluence.lib.util;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.PartEntity;
import org.confluence.lib.common.entity.PartHitTarget;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/// 弹幕共用的部位解析、伤害、范围去重、阵营与击退规则。
public final class ProjectileHitRules {
    private ProjectileHitRules() {}

    /// 校验原始碰撞部位及其遭遇主体。可攻击性、同载具、队伍友伤和服务器 PvP 均在此处处理。
    public static boolean canHit(@Nullable Entity owner, Entity rawTarget) {
        return LibEntityUtils.canHit(owner, rawTarget);
    }

    /// 生物弹幕沿用发射者的攻击限制，但以实际部位执行碰撞和伤害。
    public static boolean canMobHit(@Nullable Entity owner, Entity rawTarget) {
        LivingEntity target = logicalLivingTarget(rawTarget);
        return owner instanceof Mob mob && target != null && mob.canAttack(target) && canHit(owner, rawTarget);
    }

    /// 范围攻击包含普通实体及 PartEntity；同一去重身份只取距范围中心最近的有效部位。
    public static List<Entity> areaTargets(Level level, @Nullable Entity exclude, AABB bounds, Predicate<Entity> filter) {
        return deduplicateTargets(level.getEntities(exclude, bounds,
                entity -> acceptsAreaDamage(entity) && filter.test(entity)), bounds.getCenter());
    }

    /// 范围伤害保留物品等非射线目标；多部件实体只结算仍有效的受击部位。
    public static boolean acceptsAreaDamage(Entity target) {
        if (target.isRemoved() || !target.isAlive()) return false;
        return !(target instanceof PartHitTarget || target instanceof PartEntity<?> || target.isMultipartEntity())
                || acceptsDirectHit(target);
    }

    /// 同一伤害身份选择最近部位，独立生命部位按自己的身份保留。
    public static List<Entity> deduplicateTargets(List<Entity> candidates, Vec3 center) {
        Map<UUID, Entity> targets = new LinkedHashMap<>();
        for (Entity candidate : candidates) {
            UUID identity = dedupeIdentity(candidate).getUUID();
            Entity previous = targets.get(identity);
            if (previous == null || candidate.getBoundingBox().getCenter().distanceToSqr(center)
                    < previous.getBoundingBox().getCenter().distanceToSqr(center)) {
                targets.put(identity, candidate);
            }
        }
        return new ArrayList<>(targets.values());
    }

    /// 本体不参与拾取时，追踪其最近的有效部位；普通实体保持原目标。
    public static @Nullable Entity aimTarget(Entity target, Vec3 origin) {
        if (acceptsDirectHit(target)) return target;
        if (!target.isMultipartEntity() || target.getParts() == null) return null;
        Entity nearest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Entity part : target.getParts()) {
            if (!acceptsDirectHit(part)) continue;
            double candidateDistance = part.getBoundingBox().getCenter().distanceToSqr(origin);
            if (candidateDistance < distance) {
                nearest = part;
                distance = candidateDistance;
            }
        }
        return nearest;
    }
    /// 返回原始实体是否允许成为直接命中部位。
    public static boolean acceptsDirectHit(Entity rawTarget) {
        return LibEntityUtils.acceptsDirectHit(rawTarget);
    }

    /// 返回必须实际执行 {@link Entity#hurt} 的实体。
    /// 原始部位保留自己的护甲、伤害倍率、生命和受击反馈。
    public static Entity damageRecipient(Entity rawTarget) {
        return rawTarget instanceof PartHitTarget part ? part.damageRecipient() : rawTarget;
    }

    /// 服务端向实际部位结算伤害，不绕过部位自己的护甲、倍率和独立血量。
    public static boolean hurt(Entity rawTarget, DamageSource source, float damage) {
        if (rawTarget.level().isClientSide) return false;
        if ((rawTarget instanceof PartHitTarget || rawTarget instanceof PartEntity<?>) && !acceptsDirectHit(rawTarget)) return false;
        return damageRecipient(rawTarget).hurt(source, damage);
    }

    /// 返回部位所属的遭遇主体，用于阵营、索敌、命中特效和奖励归属。
    public static Entity encounterOwner(Entity rawTarget) {
        return LibEntityUtils.encounterOwner(rawTarget);
    }

    /// 返回一次穿透或连续碰撞中的去重身份。
    public static Entity dedupeIdentity(Entity rawTarget) {
        if (rawTarget instanceof PartHitTarget part) {
            return part.dedupeIdentity();
        }
        return encounterOwner(rawTarget);
    }

    /// 将原始碰撞实体解析为可用于召唤物锁定的生命实体。
    public static @Nullable LivingEntity logicalLivingTarget(Entity rawTarget) {
        Entity owner = encounterOwner(rawTarget);
        return owner instanceof LivingEntity living ? living : null;
    }

    /// 独立生命部位自行承受减益；普通 PartEntity 的减益交给生命主体。
    public static @Nullable LivingEntity effectTarget(Entity rawTarget) {
        Entity recipient = damageRecipient(rawTarget);
        return recipient instanceof LivingEntity living ? living : logicalLivingTarget(rawTarget);
    }

    /// 应用已经在发射快照中解析完成的击退。
    ///
    /// 这里故意不再读取攻击者当前 {@code ATTACK_KNOCKBACK}，只保留受击者的击退抗性，
    /// 从而避免换武器后改变已发射弹幕或把攻击击退属性计算两次。
    public static void applyResolvedKnockback(Entity projectile, Entity target, float strength, double motionY) {
        target = encounterOwner(target);
        if (strength <= 0.0F && motionY <= 0.0) {
            return;
        }
        double resolvedStrength = strength;
        if (target instanceof LivingEntity living) {
            AttributeInstance resistance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (resistance != null) {
                double multiplier = 1.0 - resistance.getValue();
                resolvedStrength *= multiplier;
                motionY *= multiplier;
            }
        }
        Vec3 horizontal = target.position().subtract(projectile.position()).multiply(1.0, 0.0, 1.0);
        if (horizontal.lengthSqr() < 1.0E-8) {
            horizontal = projectile.getDeltaMovement().multiply(1.0, 0.0, 1.0);
        }
        if (horizontal.lengthSqr() >= 1.0E-8 && resolvedStrength > 0.0) {
            horizontal = horizontal.normalize().scale(resolvedStrength);
        } else {
            horizontal = Vec3.ZERO;
        }
        target.addDeltaMovement(horizontal.add(0.0, motionY, 0.0));
    }
}
