package org.confluence.lib.network.s2c;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.client.handler.GravitationHandler;
import org.confluence.lib.network.IPacketS2C;

/// 向所有客户端广播「某实体是否处于重力反转」（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// 1.20 原文（`Confluence-Magic-Lib` 的 `lib/network/s2c/BroadcastGravitationRotPacketS2C.java`）逐字，
/// 原生改写 3 处（PortLib → 原生，同 `GravitationPacketC2S`）：
/// `IPortPacket.S2C` → `IPacketS2C`、`ResourceLocation ID` + `identifier()` →
/// `new Type<>(ConfluenceMagicLib.asResource("broadcast_gravitation_rot"))` + `type()`、
/// `PortStreamCodec.composite` / `PortByteBufCodecs` → 原生 `StreamCodec.composite` / `ByteBufCodecs`。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。**
/// **没有在 `LibModEvents#registerPayloadHandlers` 里登记**（本步要求「惰性」），
/// 所以 `work(...)`（→ `GravitationHandler.handleRemoteRot`）当前不会被调用。
public record BroadcastGravitationRotPacketS2C(
        int entityId,
        boolean enabled
) implements IPacketS2C {
    public static final Type<BroadcastGravitationRotPacketS2C> TYPE = new Type<>(ConfluenceMagicLib.asResource("broadcast_gravitation_rot"));
    public static final StreamCodec<ByteBuf, BroadcastGravitationRotPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BroadcastGravitationRotPacketS2C::entityId,
            ByteBufCodecs.BOOL, BroadcastGravitationRotPacketS2C::enabled,
            BroadcastGravitationRotPacketS2C::new
    );

    @Override
    public Type<BroadcastGravitationRotPacketS2C> type() {
        return TYPE;
    }

    @Override
    public void work(Player player) {
        GravitationHandler.handleRemoteRot(this, player);
    }
}
