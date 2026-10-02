package org.confluence.lib.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.effect.GravitationEffect;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.network.IPacketC2S;
import org.confluence.lib.network.s2c.BroadcastGravitationRotPacketS2C;

///
///
///
public record GravitationPacketC2S(boolean enable) implements IPacketC2S {
    public static final Type<GravitationPacketC2S> TYPE = new Type<>(ConfluenceMagicLib.asResource("gravitation"));
    public static final java.util.UUID UUID = java.util.UUID.nameUUIDFromBytes("gravitation".getBytes());
    public static final StreamCodec<ByteBuf, GravitationPacketC2S> STREAM_CODEC = ByteBufCodecs.BOOL.map(GravitationPacketC2S::new, GravitationPacketC2S::enable);

    @Override
    public Type<GravitationPacketC2S> type() {
        return TYPE;
    }

    @Override
    public void work(ServerPlayer player) {
        player.resetFallDistance();
        AttributeMap attributeMap = player.getAttributes();
        if (enable) {
            attributeMap.addTransientAttributeModifiers(GravitationEffect.GRAVITY);
        } else {
            AttributeInstance attributeInstance = attributeMap.getInstance(Attributes.GRAVITY);
            if (attributeInstance != null) attributeInstance.removeModifier(GravitationEffect.ID);
        }
        ILibEntity.of(player).confluence$setShouldRot(enable);
        PacketDistributor.sendToAllPlayers(new BroadcastGravitationRotPacketS2C(player.getId(), enable));
    }

    public static void sendToServer(boolean enable) {
        PacketDistributor.sendToServer(new GravitationPacketC2S(enable));
    }
}
