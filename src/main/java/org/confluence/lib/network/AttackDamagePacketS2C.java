package org.confluence.lib.network;

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

    /**
     * DPS 统计属于客户端运行时状态，必须交给客户端主线程更新。
     */
    @Override
    public void handle(IPortPacket.Context context) {
        Player player = context.player();
        if (player != null) context.enqueueWork(() -> work(player));
    }

    @Override
    public void work(Player player) {
        DPSMeter.addDPS(amount, player.level().getGameTime());
    }

    public static void sendToClient(ServerPlayer player, float amount) {
        // GameTest 或部分服务端模拟玩家没有真实网络连接，此时伤害数字本来就没有客户端可同步。
        if (player == null || player.connection == null) return;
        ConfluenceMagicLib.NETWORK_HANDLER.sendToPlayer(player, new AttackDamagePacketS2C(amount));
    }
}
