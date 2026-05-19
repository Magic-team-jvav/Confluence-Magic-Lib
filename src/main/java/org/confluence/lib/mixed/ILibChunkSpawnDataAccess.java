package org.confluence.lib.mixed;

import org.confluence.lib.util.NaturalSpawnerUtils;
import org.jetbrains.annotations.NotNull;

public interface ILibChunkSpawnDataAccess {
    void confluence$setData(NaturalSpawnerUtils.ChunkSpawnData data);

    NaturalSpawnerUtils.ChunkSpawnData confluence$getData();

    static @NotNull ILibChunkSpawnDataAccess of(Object o) {
        return (ILibChunkSpawnDataAccess) o;
    }
}
