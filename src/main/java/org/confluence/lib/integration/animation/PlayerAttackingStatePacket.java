package org.confluence.lib.integration.animation;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.confluence.lib.network.IPacket;

import java.util.Objects;

public record PlayerAttackingStatePacket(int playerId) implements IPacket {
    public static final Type<PlayerAttackingStatePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("cml", "pas"));
    public static final StreamCodec<ByteBuf, PlayerAttackingStatePacket> STREAM_CODEC = ByteBufCodecs.VAR_INT
            .map(PlayerAttackingStatePacket::new, PlayerAttackingStatePacket::playerId);

    @Override
    public Type<PlayerAttackingStatePacket> type() {
        return TYPE;
    }

    @Override
    public void c2s(ServerPlayer player) {
        for (ServerPlayer sp : player.serverLevel().getChunkSource().chunkMap.getPlayersWatching(player)) {
            PacketDistributor.sendToPlayer(sp, this);
        }
    }

    @Override
    public void s2c(Player player) {
        ((ILibAbstractClientPlayer) player).confluence$getAnimatable().state.isAttacking = true;
    }

    public static void sendToServer() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = Objects.requireNonNull(minecraft.player);
        if (!minecraft.options.getCameraType().isFirstPerson()) {
            ILibAbstractClientPlayer.of(player).confluence$getAnimatable().state.isAttacking = true;
        }
        PacketDistributor.sendToServer(new PlayerAttackingStatePacket(player.getId()));
    }
}
