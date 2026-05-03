package org.confluence.lib.mixed;

import org.confluence.lib.util.NaturalSpawnerUtil;
import org.jetbrains.annotations.NotNull;

public interface ILibChunkSpawnDataAccess {
    void confluence$setData(NaturalSpawnerUtil.ChunkSpawnData data);

    NaturalSpawnerUtil.ChunkSpawnData confluence$getData();

    static @NotNull ILibChunkSpawnDataAccess of(Object o) {
        return (ILibChunkSpawnDataAccess) o;
    }
}
