package org.confluence.lib.network.c2s;

import io.netty.buffer.ByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.effect.GravitationEffect;
import org.confluence.lib.mixed.ILibEntity;
import org.confluence.lib.network.s2c.BroadcastGravitationRotPacketS2C;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.common.extensions.IPortAttributesExtension;

import java.util.UUID;

public record GravitationPacketC2S(boolean enable) implements IPortPacket.C2S {
    public static final ResourceLocation ID = ConfluenceMagicLib.asResource("gravitation");
    public static final UUID UUID = java.util.UUID.nameUUIDFromBytes("gravitation".getBytes());
    public static final PortStreamCodec<ByteBuf, GravitationPacketC2S> STREAM_CODEC = PortByteBufCodecs.BOOL.map(GravitationPacketC2S::new, GravitationPacketC2S::enable);

    @Override
    public ResourceLocation identifier() {
        return ID;
    }

    @Override
    public void work(ServerPlayer player) {
        player.resetFallDistance();
        AttributeMap attributeMap = player.getAttributes();
        if (enable) {
            attributeMap.addTransientAttributeModifiers(GravitationEffect.GRAVITY);
        } else {
            AttributeInstance attributeInstance = attributeMap.getInstance(IPortAttributesExtension.gravity().value());
            if (attributeInstance != null) attributeInstance.removeModifier(GravitationEffect.ID);
        }
        ILibEntity.of(player).confluence$setShouldRot(enable);
        ConfluenceMagicLib.NETWORK_HANDLER.sendToAllPlayers(new BroadcastGravitationRotPacketS2C(player.getId(), enable));
    }

    public static void sendToServer(boolean enable) {
        ConfluenceMagicLib.NETWORK_HANDLER.sendToServer(new GravitationPacketC2S(enable));
    }
}
