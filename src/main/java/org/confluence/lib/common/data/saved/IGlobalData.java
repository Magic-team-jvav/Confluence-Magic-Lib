package org.confluence.lib.common.data.saved;

import com.mojang.serialization.Dynamic;
import net.minecraft.nbt.CompoundTag;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 用于整个存档的数据，而非维度数据（SavedData）
 */
public interface IGlobalData {
    <T> void decode(Dynamic<T> tag);

    void encode(CompoundTag nbt);

    String serializeKey();

    List<IGlobalData> DAT = new CopyOnWriteArrayList<>();

    // 必须调用这个，不然无法调用序列化
    static void registerGlobalData(IGlobalData... data) {
        DAT.addAll(Arrays.asList(data));
    }
}
