package org.confluence.lib.mixed;

import org.confluence.lib.util.NaturalSpawnerUtils;

public interface ILibChunkSpawnDataAccess {
    void confluence$setData(NaturalSpawnerUtils.ChunkSpawnData data);

    NaturalSpawnerUtils.ChunkSpawnData confluence$getData();

    static ILibChunkSpawnDataAccess of(Object o) {
        return (ILibChunkSpawnDataAccess) o;
    }
}
