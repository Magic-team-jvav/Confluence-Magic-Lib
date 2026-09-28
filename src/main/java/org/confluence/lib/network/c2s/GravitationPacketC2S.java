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

/// 客户端请求「反转/恢复重力」（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// 1.20 原文（`Confluence-Magic-Lib` 的 `lib/network/c2s/GravitationPacketC2S.java`）逐字，
/// 原生改写 3 处（都是 PortLib → 原生，与 Lib 里既有的 `AttackDamagePacketS2C` 同一套写法）：
/// 1. `IPortPacket.C2S` + `ResourceLocation ID` + `identifier()` → `IPacketC2S` +
///    `new Type<>(ConfluenceMagicLib.asResource("gravitation"))` + `type()`
///    （包 id 沿用 1.20 的 `confluence_magic_lib:gravitation`，未改成其他 Lib 包用的 `cml:` 简写 ——
///    保持 1.20 事实来源；两者只是通道名风格不同，不冲突）；
/// 2. `PortStreamCodec` / `PortByteBufCodecs` → 原生 `StreamCodec` / `ByteBufCodecs`（`ByteBuf` 缓冲区）；
/// 3. `PortPacketDistributor.sendToAllPlayers(...)` → 原生 `PacketDistributor.sendToAllPlayers(...)`。
///
/// 另有 1 处 1.21 原生 API 差异（实测）：`attributeMap.getInstance(...)` 在 1.21 收的是
/// `Holder<Attribute>` —— 1.20 写 `Attributes.GRAVITY.value()`（Forge 的 `LazyHolder`），
/// 1.21.1 原版 `Attributes.GRAVITY` **本身就是** `Holder<Attribute>`（`Attributes.java:74`），
/// 所以去掉 `.value()`（`GravitationEffect` 的类注释里记着同一条）。
///
/// ⚠️ **本类已落地但尚未接线，接线见 WP6c 第二步。**
/// **没有在 `LibModEvents#registerPayloadHandlers` 里登记**（本步要求「惰性」），
/// 因此 `sendToServer(...)` 发的包当前无人注册、`work(...)` 不会被调用；
/// `GravitationHandler` 里的调用点同理（见该类注释）。第二步登记时才生效。
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
