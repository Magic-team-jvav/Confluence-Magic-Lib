package org.confluence.lib.mixed;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;

public interface ILibClientboundUpdateMobEffectPacket {
    boolean confluence$isEnabled();

    static ILibClientboundUpdateMobEffectPacket of(ClientboundUpdateMobEffectPacket packet) {
        return (ILibClientboundUpdateMobEffectPacket) packet;
    }
}
