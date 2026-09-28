package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/// `LivingEntity` 上的注入。
///
/// ## 1.21 既有内容（**未改动**）
///
/// `armorPenetration` —— 护甲穿透（`getDamageAfterArmorAbsorb` 的 `CombatRules.getDamageAfterAbsorb`）。
///
/// ## WP6c 合并进来的「重力反转」两处（来自 1.20 `Confluence-Magic-Lib` 的
/// `lib/mixin/LivingEntityMixin.java`）
///
/// | 处理器 | 1.20 位置 | 1.21 目标（实测） |
/// |---|---|---|
/// | `modifyParticlePosY` | 1.20 `LivingEntityMixin:47-54` | `LivingEntity#checkFallDamage(double,boolean,BlockState,BlockPos)` 里的 `ServerLevel#sendParticles(ParticleOptions,DDDIDDDD)I`：实参序 `(粒子, x, y, z, 数量, ...)` → `index = 2` 是 **y** |
/// | `reversed` | 1.20 `LivingEntityMixin:56-62` | `LivingEntity#travel(Vec3)` 的 `argsOnly` 首参 |
///
/// 1.20 那两处的其余成员（`confused` / `hasEffect` / `getEffect` / `shouldAdd` / `getActiveEffectsMap` 等）
/// **1.21 已经没有了**（`LibEffects.CONFUSED` 那套走的是别的实现），所以只合并这两处，其余保持 1.21 原样。
///
/// 为此给本类补了 `implements SelfGetter<LivingEntity>`（1.20 那份是通过 `ILibLivingEntity` 间接拿到
/// `confluence$self()` 的；1.21 这个类此前没有继承任何 Lib 接口）—— 与 Lib 既有的
/// `mixin/client/LocalPlayerMixin implements SelfGetter<LocalPlayer>` 同一写法。
///
/// ⚠️ **合并进来的两处已落地但尚未接线，接线见 WP6c 第二步。**
/// `confluence$isShouldRot()` 恒为 `false` → 两处都是 no-op（`modifyParticlePosY` 原样返回、
/// `reversed` 原样返回），行为与合并前**零变化**。
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements SelfGetter<LivingEntity> {
    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(Lnet/minecraft/world/entity/LivingEntity;FLnet/minecraft/world/damagesource/DamageSource;FF)F"))
    private float armorPenetration(LivingEntity entity, float damage, DamageSource damageSource, float armorValue, float armorToughness, Operation<Float> original) {
        return original.call(entity, damage, damageSource, LibAttributes.applyArmorPenetration(entity, damageSource, armorValue), armorToughness);
    }

    // ===== WP6c：下面两个处理器来自 1.20 Lib 的 LivingEntityMixin（重力反转） =====

    /// 1.20 `LivingEntityMixin:47-54`，逐字（只把 `ILibEntity` 的 import 换到 Lib 新位置）。
    @ModifyArg(method = "checkFallDamage", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"), index = 2)
    private double modifyParticlePosY(double posY) {
        ILibEntity self = ILibEntity.of(confluence$self());
        if (self.confluence$isShouldRot()) {
            return posY + self.confluence$getDimensionHeight() - 0.15;
        }
        return posY;
    }

    /// 1.20 `LivingEntityMixin:56-62`，逐字。
    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 reversed(Vec3 vec3) {
        if (ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            return new Vec3(-vec3.x, vec3.y, vec3.z);
        }
        return vec3;
    }
}
