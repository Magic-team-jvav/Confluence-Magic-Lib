package org.confluence.lib.util;

import com.google.common.util.concurrent.AtomicDouble;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.dimension.LevelStem;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物刷新工具类
 */
public final class NaturalSpawnerUtil {
    public static final int DEFAULT_MULTIPLIER = 1;
    private static final Map<ResourceKey<Level>, Map<UUID, PlayerEnemySpawnData>> PLAYER_ENEMY_SPAWN_DATA = new IdentityHashMap<>(); // 不用concurrent是因为在服务器启动时就初始化了
    private static Set<ResourceKey<Level>> UNKNOWN_DIMENSIONS;

    public static @Nullable PlayerEnemySpawnData getEnemySpawnData(Player player) {
        Map<UUID, PlayerEnemySpawnData> map = PLAYER_ENEMY_SPAWN_DATA.get(player.level().dimension());
        return map == null ? null : map.get(player.getUUID());
    }

    public static int confluenceLib$canSpawn(int original, MobCategory category, ServerPlayer player) {
        PlayerEnemySpawnData data;
        if (category.isPersistent() || player == null || (data = NaturalSpawnerUtil.getEnemySpawnData(player)) == null) {
            return original;
        }
        return original * Mth.ceil(data.getCountMultiplier());
    }

    public static int confluenceLib$canSpawnForCategory(int original, MobCategory category, ChunkPos pos, ServerLevel level) {
        Player player;
        NaturalSpawnerUtil.PlayerEnemySpawnData data;
        if (category.isPersistent() ||
                level == null ||
                (player = level.getNearestPlayer(pos.x, 0, pos.z, -1, false)) == null ||
                (data = NaturalSpawnerUtil.getEnemySpawnData(player)) == null
        ) {
            return original;
        }
        return Mth.ceil(original * data.getCountMultiplier());
    }

    public static void init(MinecraftServer server) {
        Registry<LevelStem> registry = server.registries().compositeAccess().registryOrThrow(Registries.LEVEL_STEM);
        for (ResourceKey<LevelStem> key : registry.registryKeySet()) {
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, key.location());
            PLAYER_ENEMY_SPAWN_DATA.put(dimension, new ConcurrentHashMap<>());
        }
    }

    public static void update(MinecraftServer server) {
        if (server.getWorldData().overworldData().getGameTime() % (5 * 20) == 0) {
            Map<ResourceKey<Level>, Set<UUID>> multiMap = new IdentityHashMap<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ResourceKey<Level> dimension = player.level().dimension();
                Map<UUID, PlayerEnemySpawnData> map = PLAYER_ENEMY_SPAWN_DATA.get(dimension);
                if (map == null) {
                    if (UNKNOWN_DIMENSIONS == null) {
                        UNKNOWN_DIMENSIONS = new HashSet<>();
                    }
                    if (UNKNOWN_DIMENSIONS.add(dimension)) {
                        ConfluenceMagicLib.LOGGER.warn("Why there's a new dimension '{}' here?", dimension.location());
                    }
                    continue;
                }
                map.computeIfAbsent(player.getUUID(), u -> new PlayerEnemySpawnData()).initOrUpdate(player);
                multiMap.computeIfAbsent(dimension, k -> new HashSet<>()).add(player.getUUID());
            }
            for (Map.Entry<ResourceKey<Level>, Set<UUID>> entry : multiMap.entrySet()) {
                PLAYER_ENEMY_SPAWN_DATA.get(entry.getKey()).keySet().removeIf(uuid -> !entry.getValue().contains(uuid));
            }
        }
    }

    public static void clear() {
        PLAYER_ENEMY_SPAWN_DATA.clear();
    }

    public static boolean confluenceLib$wrap(boolean original,
                                             MobCategory category,
                                             ServerLevel level,
                                             BlockPos pos,
                                             ChunkAccess chunk,
                                             int k,
                                             BlockPos.MutableBlockPos mutablePos,
                                             LocalIntRef frequency,
                                             LocalBooleanRef isObtain) {
        boolean obtain = isObtain.get();
        if (category.isPersistent() || obtain && frequency == null) {
            return original;
        }
        if (obtain) {
            return k < frequency.get();
        }

        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), -1.0, false);
        PlayerEnemySpawnData data;
        if (player == null ||
                (data = getEnemySpawnData(player)) == null ||
                !NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint(level, chunk, mutablePos, data.distanceToSqr(pos))
        ) {
            return original;
        }

        frequency.set(Mth.ceil(3 * data.getSpeedMultiplier()));
        isObtain.set(true);

        return k < frequency.get();
    }

    public static final class PlayerEnemySpawnData {
        private final AtomicDouble x = new AtomicDouble();
        private final AtomicDouble y = new AtomicDouble();
        private final AtomicDouble z = new AtomicDouble();
        private final AtomicDouble speedMultiplier = new AtomicDouble(DEFAULT_MULTIPLIER);
        private final AtomicDouble countMultiplier = new AtomicDouble(DEFAULT_MULTIPLIER);

        private void initOrUpdate(ServerPlayer player) {
            speedMultiplier.set(player.getAttributeValue(ConfluenceMagicLib.MOB_SPAWN_SPEED_MULTIPLIER));
            countMultiplier.set(player.getAttributeValue(ConfluenceMagicLib.MOB_SPAWN_COUNT_MULTIPLIER));
            x.set(player.getX());
            y.set(player.getY());
            z.set(player.getZ());
        }

        public double getSpeedMultiplier() {
            return speedMultiplier.get();
        }

        public double getCountMultiplier() {
            return countMultiplier.get();
        }

        public double distanceToSqr(double x, double y, double z) {
            return Mth.lengthSquared(getX() - x, getY() - y, getZ() - z);
        }

        public double distanceToSqr(Position position) {
            return distanceToSqr(position.x(), position.y(), position.z());
        }

        public double distanceToSqr(BlockPos pos) {
            return distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
        }

        public double getX() {
            return x.get();
        }

        public double getY() {
            return y.get();
        }

        public double getZ() {
            return z.get();
        }
    }
}
