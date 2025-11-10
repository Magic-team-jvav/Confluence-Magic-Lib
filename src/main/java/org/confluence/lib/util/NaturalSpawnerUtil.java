package org.confluence.lib.util;

import com.google.common.util.concurrent.AtomicDouble;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 生物刷新工具类
 */
public final class NaturalSpawnerUtil {

    private static final ConcurrentHashMap<ResourceKey<Level>, ConcurrentHashMap<Player, PlayerEnemySpawnData>> PLAYER_ENEMY_SPAWN_DATA = new ConcurrentHashMap<>();

    @Nullable
    public static PlayerEnemySpawnData getEnemySpawnData(Player player) {
        return PLAYER_ENEMY_SPAWN_DATA.get(player.level().dimension()).get(player);
    }

    public static int confluenceLib$canSpawn(final int original, final MobCategory category, final ServerPlayer serverPlayer) {
        if (category.isFriendly() || serverPlayer == null) {
            return original;
        }
        var data = NaturalSpawnerUtil.getEnemySpawnData(serverPlayer);
        return data == null ? original : original * Mth.ceil(data.getCountMultiplier());
    }

    public static int confluenceLib$canSpawnForCategory(final int o, final MobCategory category, final ChunkPos pos, final ServerLevel serverLevel) {
        if (category.isFriendly() || serverLevel == null) {
            return o;
        }
        Player player = serverLevel.getNearestPlayer(pos.x, 0, pos.z, -1, false);
        if (player == null) {
            return o;
        }
        NaturalSpawnerUtil.PlayerEnemySpawnData data = NaturalSpawnerUtil.getEnemySpawnData(player);
        return data == null ? o : Mth.ceil(o * data.getCountMultiplier());
    }

    public static void initOrUpdate(ServerLevel level) {
        var map = PLAYER_ENEMY_SPAWN_DATA.computeIfAbsent(level.dimension(), (l) -> new ConcurrentHashMap<>());
        var players = level.players();
        map.keySet().removeIf(element -> players.stream().noneMatch(a -> a.getUUID().equals(element.getUUID())));
        players.forEach(player -> map.computeIfAbsent(player, (p) -> new PlayerEnemySpawnData()).initOrUpdate(player));
    }

    public static boolean confluenceLib$wrap(final boolean original,
                                             final MobCategory category,
                                             final ServerLevel level,
                                             final BlockPos pos,
                                             final ChunkAccess chunk,
                                             final int k,
                                             final BlockPos.MutableBlockPos blockpos$mutableblockpos,
                                             final LocalIntRef frequency,
                                             final LocalBooleanRef isObtain) {
        if (category.isFriendly()) {
            return original;
        }
        if (isObtain.get()) {
            return frequency != null ? k < frequency.get() : original;
        }

        Player player = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), -1.0, false);
        PlayerEnemySpawnData data;
        if (player == null ||
                (data = getEnemySpawnData(player)) == null ||
                !NaturalSpawner.isRightDistanceToPlayerAndSpawnPoint(level, chunk, blockpos$mutableblockpos, data.distanceToSqr(pos))) {
            return original;
        }
        frequency.set(Mth.ceil(3 * data.getSpeedMultiplier()));
        isObtain.set(true);
        return k < frequency.get();
    }

    public static final class PlayerEnemySpawnData {
        private final AtomicDouble x = new AtomicDouble(0);
        private final AtomicDouble y = new AtomicDouble(0);
        private final AtomicDouble z = new AtomicDouble(0);
        private final AtomicDouble speedMultiplier = new AtomicDouble(ConfluenceMagicLib.ENEMY_SPAWN_SPEED_MULTIPLIER.get().getDefaultValue());
        private final AtomicDouble countMultiplier = new AtomicDouble(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER.get().getDefaultValue());

        private void initOrUpdate(ServerPlayer player) {
            this.speedMultiplier.set(player.getAttributeValue(ConfluenceMagicLib.ENEMY_SPAWN_SPEED_MULTIPLIER));
            this.countMultiplier.set(player.getAttributeValue(ConfluenceMagicLib.ENEMY_SPAWN_COUNT_MULTIPLIER));
            this.x.set(player.getX());
            this.y.set(player.getY());
            this.z.set(player.getZ());
        }

        public double getSpeedMultiplier() {
            return this.speedMultiplier.get();
        }

        public double getCountMultiplier() {
            return this.countMultiplier.get();
        }

        public double distanceToSqr(double x, double y, double z) {
            double d0 = this.getX() - x;
            double d1 = this.getY() - y;
            double d2 = this.getZ() - z;
            return d0 * d0 + d1 * d1 + d2 * d2;
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
