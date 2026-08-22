package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.confluence.lib.mixed.IClientboundUpdateMobEffectPacket;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientboundUpdateMobEffectPacket.class, priority = 1145)
public abstract class ClientboundUpdateMobEffectPacketMixin implements IClientboundUpdateMobEffectPacket {
    @Unique
    private boolean confluence$enabled = true;

    @Override
    public boolean confluence$isEnabled() {
        return confluence$enabled;
    }

    @Inject(method = "<init>(ILnet/minecraft/world/effect/MobEffectInstance;)V", at = @At("TAIL"))
    private void init(CallbackInfo ci, @Local(argsOnly = true) MobEffectInstance effect) {
        this.confluence$enabled = ILibMobEffectInstance.of(effect).confluence$isEnabled();
    }

    @Inject(method = "<init>(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("TAIL"))
    private void init(FriendlyByteBuf buffer, CallbackInfo ci) {
        this.confluence$enabled = buffer.readBoolean();
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void encode(FriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeBoolean(confluence$enabled);
    }
}
