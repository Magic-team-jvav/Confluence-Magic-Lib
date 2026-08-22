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
    private boolean terra_curio$isShouldRot = false;
    @Unique
    private float terra_curio$dimensionHeight = 0.0F;

    @Override
    public void confluence$setShouldRot(boolean bool) {
        this.terra_curio$isShouldRot = bool;
    }

    @Override
    public boolean confluence$isShouldRot() {
        return terra_curio$isShouldRot;
    }

    @Override
    public float confluence$getDimensionHeight() {
        return terra_curio$dimensionHeight;
    }

    @Inject(method = "baseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;isInLava()Z", ordinal = 1))
    private void cacheDimensionHeight(CallbackInfo ci) {
        this.terra_curio$dimensionHeight = terra_curio$isShouldRot ? getDimensions(getPose()).height : 0.0F;
    }

    @Inject(method = "getOnPosLegacy", at = @At("RETURN"), cancellable = true)
    private void getOnPosAbove(CallbackInfoReturnable<BlockPos> cir) {
        if (terra_curio$isShouldRot) {
            cir.setReturnValue(getOnPos(-(terra_curio$dimensionHeight + 0.2F)));
        }
    }

    @WrapOperation(method = "checkSupportingBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getBoundingBox()Lnet/minecraft/world/phys/AABB;"))
    private AABB getBoundingBox(Entity instance, Operation<AABB> original) {
        AABB aabb = original.call(instance);
        if (terra_curio$isShouldRot) {
            return new AABB(aabb.minX, aabb.maxY + Mth.EPSILON, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        }
        return aabb;
    }

    @Inject(method = "checkFallDamage", at = @At("TAIL"))
    private void updateFallDistance(CallbackInfo ci, @Local(argsOnly = true) double y, @Local(argsOnly = true) boolean onGround) {
        if (terra_curio$isShouldRot && !onGround) {
            if (y > 0.0) {
                this.fallDistance += (float) y;
            } else {
                this.fallDistance = 0.0F;
            }
        }
    }

    @ModifyArg(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), index = 2)
    private double modifyParticlePosY(double y) {
        if (terra_curio$isShouldRot) {
            return y - 0.2 + terra_curio$dimensionHeight;
        }
        return y;
    }

    @ModifyArg(method = "spawnSprintParticle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"), index = 5)
    private double modifyParticleSpeedY(double y) {
        return terra_curio$isShouldRot ? -y : y;
    }

    @Inject(method = "move", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/Entity;verticalCollisionBelow:Z", opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER))
    private void flip(MoverType type, Vec3 pos, CallbackInfo ci) {
        if (ILibEntity.of(confluence$self()).confluence$isShouldRot()) {
            this.verticalCollisionBelow = verticalCollision && pos.y > 0.0;
        }
    }
}
