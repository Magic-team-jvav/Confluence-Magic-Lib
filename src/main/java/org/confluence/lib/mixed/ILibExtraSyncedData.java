package org.confluence.lib.mixed;

import net.minecraft.world.entity.Entity;
import org.confluence.lib.network.s2c.SetEntityDataPacketS2C;
import org.mesdag.portlib.network.PortPacketDistributor;

/// @see SetEntityDataPacketS2C
public interface ILibExtraSyncedData<T extends Entity> extends SelfGetter<T> {
    /// Must Not Be Invoked By Overriders
    default void confluence$setData(byte dataId, Object o) {
        defaultSetData(confluence$self(), dataId, o);
    }

    static void defaultSetData(Entity self, byte dataId, Object o) {
        if (!self.level().isClientSide) {
            PortPacketDistributor.sendToPlayersTrackingEntity(self, new SetEntityDataPacketS2C(self.getId(), new SetEntityDataPacketS2C.Entry(dataId, o)));
        }
    }

    Object confluence$getData(byte dataId);

    byte[] confluence$getAllDataId();

    default SetEntityDataPacketS2C.Entry[] confluence$getAllEntries() {
        byte[] dataIds = confluence$getAllDataId();
        SetEntityDataPacketS2C.Entry[] entries = new SetEntityDataPacketS2C.Entry[dataIds.length];
        for (int i = 0; i < dataIds.length; i++) {
            entries[i] = new SetEntityDataPacketS2C.Entry(dataIds[i], confluence$getData(dataIds[i]));
        }
        return entries;
    }
}
