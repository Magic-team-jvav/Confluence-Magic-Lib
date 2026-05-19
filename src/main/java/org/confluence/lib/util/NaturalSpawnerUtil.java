package org.confluence.lib.util;

import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@Deprecated(since = "1.3.0", forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.4.0")
public final class NaturalSpawnerUtil {
    public static NaturalSpawnerUtils.ChunkSpawnData getChunkSpawnData(ResourceKey<Level> dimension, ChunkPos pos) {
        return NaturalSpawnerUtils.getChunkSpawnData(dimension, pos);
    }

    public static @Nullable Long2ObjectMap<NaturalSpawnerUtils.ChunkSpawnData> getDimensionChunkSpawnData(ResourceKey<Level> dimension) {
        return NaturalSpawnerUtils.getDimensionChunkSpawnData(dimension);
    }

    public static void init(MinecraftServer server) {
        NaturalSpawnerUtils.init(server);
    }

    public static void update(MinecraftServer server) {
        NaturalSpawnerUtils.update(server);
    }

    public static void clear() {
        NaturalSpawnerUtils.clear();
    }

    public static boolean modifySpeed(
            boolean original,
            MobCategory category,
            int k,
            LocalIntRef frequency,
            LocalBooleanRef obtained
    ) {
        return NaturalSpawnerUtils.modifySpeed(original, category, k, frequency, obtained);
    }
}
