package org.confluence.lib.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.mixed.ILibExtraSyncedData;
import org.mesdag.portlib.network.IPortPacket;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortStreamCodec;

public record SetEntityDataPacketS2C(int entityId, Entry... entries) implements IPortPacket.S2C {
    public static final byte DATA_BOOLEAN = 0;
    public static final ResourceLocation ID = ConfluenceMagicLib.asResource("set_entity_data");
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, SetEntityDataPacketS2C> STREAM_CODEC = new PortStreamCodec<>() {
        @Override
        public SetEntityDataPacketS2C decode(PortRegistryFriendlyByteBuf buffer) {
            int entityId = buffer.readVarInt();
            int size = buffer.readVarInt();
            Entry[] entries = new Entry[size];
            for (int i = 0; i < size; i++) {
                byte dataId = buffer.readByte();
                entries[i] = new Entry(dataId, decodeData(buffer, dataId));
            }
            return new SetEntityDataPacketS2C(entityId, entries);
        }

        @Override
        public void encode(PortRegistryFriendlyByteBuf buffer, SetEntityDataPacketS2C value) {
            buffer.writeVarInt(value.entityId);
            buffer.writeVarInt(value.entries.length);
            for (Entry entry : value.entries) {
                buffer.writeByte(entry.dataId);
                encodeData(buffer, entry.dataId, entry.data);
            }
        }
    };

    @Override
    public ResourceLocation identifier() {
        return ID;
    }

    /**
     * 扩展同步字段会修改客户端实体，必须交给客户端主线程执行。
     */
    @Override
    public void handle(IPortPacket.Context context) {
        Player player = context.player();
        if (player != null) context.enqueueWork(() -> work(player));
    }

    @Override
    public void work(Player player) {
        Entity entity = player.level().getEntity(entityId);
        if (entity instanceof ILibExtraSyncedData<?> extraSyncedData) {
            for (Entry entry : entries) {
                extraSyncedData.confluence$setData(entry.dataId, entry.data);
            }
        }
    }

    private static Object decodeData(PortRegistryFriendlyByteBuf buffer, byte dataId) {
        if (dataId == DATA_BOOLEAN) return buffer.readBoolean();
        throw new IllegalArgumentException("Unregistered data serializer id " + dataId + "!");
    }

    private static void encodeData(PortRegistryFriendlyByteBuf buffer, byte dataId, Object o) {
        if (dataId == DATA_BOOLEAN) buffer.writeBoolean((boolean) o);
        else throw new IllegalArgumentException("Unregistered data serializer id " + dataId + "!");
    }

    public record Entry(byte dataId, Object data) {}

    public static Entry ofBoolean(boolean data) {
        return new Entry(DATA_BOOLEAN, data);
    }

}
