package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.mixed.ILibEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/// `Entity` 上的重力反转注入（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// ## ⚠️ 与 1.20 的**唯一**一处非逐字改动：`@Unique` 字段改名
///
/// 1.20 这份用的是**历史遗留命名** `terra_curio$isShouldRot` / `terra_curio$dimensionHeight`
/// （这条特性当年先在 TerraCurio 里写、后来才搬进 Lib，前缀没跟着改）。而 **1.21 的 TerraCurio
/// `mixin/EntityMixin.java` 里有一模一样的字段名**（实测：`terra_curio$isShouldRot` 在 `:50`、
/// `terra_curio$dimensionHeight` 在 `:52`）。两个 `@Mixin(Entity.class)` 各自声明同名的 `@Unique`
/// 字段 → 注入到同一个 `Entity` 上时**字段重复、启动即崩**。
///
/// 所以本类把它们改名为 **`confluence$isShouldRot` / `confluence$dimensionHeight`**，
/// 与 `ILibEntity` 的方法前缀一致。`@Unique` 私有字段不对外可见（只被本类与 `ILibEntity`
/// 的方法实现访问），改名**零外部影响**；第二步做原子切换（删掉 TerraCurio 那份）时也不需要再改回来。
///
/// ## 本类只搬「重力」相关成员
///
/// 1.20 那份 Lib `EntityMixin` 本身就是重力专用（没有 cthulhu 冲刺 / `TCUtils.applyLavaImmune` /
/// `isPlayer` —— 那些留在 TerraCurio 的 `IEntity`/`EntityMixin` 里），所以是整份逐字搬。
///
/// ## 注入点（每个都在 1.21 源码里逐个核实过）
///
/// | 注入 | 1.21 目标（实测） |
/// |---|---|
/// | `cacheDimensionHeight` | `Entity#baseTick` 的 `isInLava()` INVOKE **ordinal = 1** —— 1.21 `baseTick` 里 `isInLava()` 正好出现两次（`:467` 的火焰判定、`:480` 的 `lavaHurt`），ordinal 1 命中后者 |
/// | `getOnPosAbove` | `Entity#getOnPosLegacy()`（`Entity.java` 中存在，public） |
/// | `getBoundingBox` | `Entity#checkSupportingBlock(boolean, Vec3)` 里的 `Entity#getBoundingBox()` INVOKE |
/// | `updateFallDistance` | `Entity#checkFallDamage(double y, boolean onGround, BlockState state, BlockPos pos)` @TAIL，两个 `@Local(argsOnly = true)` 分别取 y / onGround |
/// | `modifyParticlePosY` / `modifyParticleSpeedY` | `Entity#spawnSprintParticle()` 里的 `Level#addParticle(ParticleOptions, DDDDDD)`，实参序为 `(粒子, x, y, z, dx, dy, dz)` → index 2 = y、index 5 = dy |
/// | `flip` | `Entity#move(MoverType, Vec3)` 中 `verticalCollisionBelow` 的 PUTFIELD（1.21 该字段只被写一次） |
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。**
/// 本类注入的读取点全是 `confluence$isShouldRot()`，而写入它的唯一入口是
/// `GravitationPacketC2S`（**尚未注册**）→ 接线前该值恒为 `false`，**所有注入都是 no-op**，
/// 行为与接线前**零变化**（TerraCurio 那份仍在独立工作，用的是它自己的 `terra_curio$` 字段）。
@Mixin(Entity.class)
public abstract class EntityMixin implements ILibEntity {
    @Shadow
    public abstract EntityDimensions getDimensions(Pose pose);

    @Shadow
    public abstract Pose getPose();

    @Shadow
    protected abstract BlockPos getOnPos(float yOffset);

    @Shadow
    public float fallDistance;
    @Shadow
    public boolean verticalCollisionBelow;
    @Shadow
    public boolean verticalCollision;
    @Unique
    private boolean confluence$isShouldRot = false;
    @Unique
    private float confluence$dimensionHeight = 0.0F;

    @Override
    public void confluence$setShouldRot(boolean bool) {
        this.confluence$isShouldRot = bool;
    }

    @Override
    public boolean confluence$isShouldRot() {
        return confluence$isShouldRot;
    }

    @Override
    public float confluence$getDimensionHeight() {
        return confluence$dimensionHeight;
    }

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInLava()Z", ordinal = 1))
    private void cacheDimensionHeight(CallbackInfo ci) {
        // 1.21 API 差异：`EntityDimensions` 在 1.21 是 **record**
        // （`public record EntityDimensions(float width, float height, ...)`），
        // 所以 1.20 的 `.height` 字段读取必须写成访问器 `.height()`（原版 `Entity.java:2985/2987` 也是这么用的）。
        this.confluence$dimensionHeight = confluence$isShouldRot ? getDimensions(getPose()).height() : 0.0F;
    }

    @Inject(method = "getOnPosLegacy", at = @At("RETURN"), cancellable = true)
    private void getOnPosAbove(CallbackInfoReturnable<BlockPos> cir) {
        if (confluence$isShouldRot) {
            cir.setReturnValue(getOnPos(-(confluence$dimensionHeight + 0.2F)));
        }
    }

    @WrapOperation(method = "checkSupportingBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB getBoundingBox(Entity instance, Operation<AABB> original) {
        AABB aabb = original.call(instance);
        if (confluence$isShouldRot) {
            return new AABB(aabb.minX, aabb.maxY + Mth.EPSILON, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        }
        return aabb;
    }

    @Inject(method = "checkFallDamage", at = @At("TAIL"))
    private void updateFallDistance(CallbackInfo ci, @Local(argsOnly = true) double y, @Local(argsOnly = true) boolean onGround) {
        if (confluence$isShouldRot && !onGround) {
            if (y > 0.0) {
                this.fallDistance += (float) y;
            } else {
                this.fallDistance = 0.0F;
            }
        }
    }

    @ModifyArg(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), index = 2)
    private double modifyParticlePosY(double y) {
        if (confluence$isShouldRot) {
            return y - 0.2 + confluence$dimensionHeight;
        }
        return y;
    }

    @ModifyArg(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), index = 5)
    private double modifyParticleSpeedY(double y) {
        return confluence$isShouldRot ? -y : y;
    }

    @Inject(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;verticalCollisionBelow:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void flip(MoverType type, Vec3 pos, CallbackInfo ci) {
        if (ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            this.verticalCollisionBelow = verticalCollision && pos.y > 0.0;
        }
    }
}
