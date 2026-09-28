package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.mixed.SelfGetter;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// 重力反转时 `Player#maybeBackOffFromEdge`（防掉边逻辑）要**反向**判断，以及跳跃方向要翻转。
///
/// WP6c：1.20 原文（`Confluence-Magic-Lib` 的 `lib/mixin/client/ClientPlayerMixin.java`）逐字。
///
/// 注入点核实（1.21 源码，`Player#maybeBackOffFromEdge(Vec3 vec, MoverType mover)`）：
/// - `vec.y` 的 `GETFIELD`：1.21 方法体里 `Vec3.y` 出现两次（`:1149` 的条件、`:1189` 的 `new Vec3(d0, vec.y, d1)`）
///   → `ordinal = 0` 命中前者，与 1.20 一致；
/// - `this.maxUpStep()`：**1.21 的 `Player` 并未覆写 `maxUpStep()`**（它声明在 `Entity:3624` / `LivingEntity:3729`，
///   `Player:1147` 只是调用点）。但 **javac 对继承来的方法调用，INVOKE 的 owner 用的是「接收者的静态类型」**
///   （已用 `javac`+`javap` 实测：`class Mid extends Base { void n(){ this.inherited(); } }` 编出的是
///   `Methodref Mid.inherited`，不是 `Base.inherited`）→ 这里 owner 仍是 `Player`，
///   1.20 的 target `Lnet/minecraft/world/entity/player/Player;maxUpStep()F` **在 1.21 依然匹配**；
/// - `Player#jumpFromGround()` 存在。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。** `confluence$isShouldRot()` 恒为 `false` → 三处全 no-op。
@Mixin(Player.class)
public abstract class ClientPlayerMixin implements SelfGetter<Player> {
    @ModifyExpressionValue(method = "maybeBackOffFromEdge", at = @At(value = "FIELD", target = "Lnet/minecraft/world/phys/Vec3;y:D", opcode = Opcodes.GETFIELD, ordinal = 0))
    private double invert(double original) {
        return ILibEntity.of(confluence$self()).confluence$isShouldRot() ? -original : original;
    }

    @WrapOperation(method = "maybeBackOffFromEdge", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;maxUpStep()F"))
    private float maxDownStep(Player instance, Operation<Float> original) {
        float maxUpStep = original.call(instance);
        ILibEntity self = ILibEntity.of(instance);
        if (self.confluence$isShouldRot()) {
            return -(Mth.EPSILON + Mth.EPSILON) - maxUpStep - self.confluence$getDimensionHeight();
        }
        return maxUpStep;
    }

    @Inject(method = "jumpFromGround", at = @At("TAIL"))
    private void flipJump(CallbackInfo ci) {
        Player self = confluence$self();
        if (ILibEntity.of(self).confluence$isShouldRot()) {
            Vec3 vec3 = self.getDeltaMovement();
            self.setDeltaMovement(vec3.x, -vec3.y, vec3.z);
        }
    }
}
