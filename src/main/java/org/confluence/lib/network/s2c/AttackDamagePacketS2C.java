package org.confluence.lib.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.client.DPSMeter;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

public record AttackDamagePacketS2C(float amount) implements IPortPacket.S2C {
    public static final ResourceLocation ID = ConfluenceMagicLib.asResource("attack_damage");
    public static final PortStreamCodec<ByteBuf, AttackDamagePacketS2C> STREAM_CODEC = PortByteBufCodecs.FLOAT.map(AttackDamagePacketS2C::new, AttackDamagePacketS2C::amount);

    @Override
    public ResourceLocation identifier() {
        return ID;
    }

    @Override
    public void work(Player player) {
        DPSMeter.addDPS(amount, player.level().getGameTime());
    }

    public static void sendToClient(ServerPlayer player, float amount) {
        ConfluenceMagicLib.NETWORK_HANDLER.sendToPlayer(player, new AttackDamagePacketS2C(amount));
    }
}
