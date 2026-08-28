package org.confluence.lib.integration.animation;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.ConfluenceMagicLib;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.Objects;

public record PlayerAttackingStatePacket(int playerId) implements IPortPacket {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("cml", "pas");
    public static final PortStreamCodec<ByteBuf, PlayerAttackingStatePacket> STREAM_CODEC = PortByteBufCodecs.VAR_INT
            .map(PlayerAttackingStatePacket::new, PlayerAttackingStatePacket::playerId);

    public static void sendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = Objects.requireNonNull(minecraft.player);
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            ILibAbstractClientPlayer.of(player).confluence$getAnimatable().state.isAttacking = true;
        }
        ConfluenceMagicLib.NETWORK_HANDLER.sendToServer(new PlayerAttackingStatePacket(player.getId()));
    }

    @Override
    public void handle(Context context) {
        if (context.player() == null) return;
        Entity player = context.player().level().getEntity(playerId);
        if (player instanceof ServerPlayer sp) {
            sp.serverLevel().getChunkSource().broadcast(sp, ConfluenceMagicLib.NETWORK_HANDLER.toVanillaClientbound(this));
        } else if (player instanceof Player) {
            ((ILibAbstractClientPlayer) player).confluence$getAnimatable().state.isAttacking = true;
        }
    }

    @Override
    public ResourceLocation identifier() {
        return ID;
    }
}
