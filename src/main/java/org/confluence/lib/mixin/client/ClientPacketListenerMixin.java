package org.confluence.lib.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.world.effect.MobEffectInstance;
import org.confluence.lib.mixed.IClientboundUpdateMobEffectPacket;
import org.confluence.lib.mixed.ILibMobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @ModifyExpressionValue(method = "handleUpdateMobEffect", at = @At(value = "NEW", target = "(Lnet/minecraft/world/effect/MobEffect;IIZZZLnet/minecraft/world/effect/MobEffectInstance;Ljava/util/Optional;)Lnet/minecraft/world/effect/MobEffectInstance;"))
    private MobEffectInstance modify(MobEffectInstance original, @Local(argsOnly = true) ClientboundUpdateMobEffectPacket packet) {
        ILibMobEffectInstance.of(original).confluence$setEnabled(IClientboundUpdateMobEffectPacket.of(packet).confluence$isEnabled());
        return original;
    }
}
