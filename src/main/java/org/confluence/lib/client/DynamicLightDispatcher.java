package org.confluence.lib.client;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/// 动态光照调度器。每帧提交光源，帧末对比位置与亮度并刷新受影响的区块。
@SuppressWarnings("unused")
public final class DynamicLightDispatcher {
    private static final double RADIUS = 7.75;
    private static final double MAX_RADIUS_SQUARED = RADIUS * RADIUS;
    private static final int SECTION_BITS = 4;
    private static final Map<Object, LightSource> FrameLightSources = new HashMap<>();
    private static final Map<Long, RegisteredSource> RegisteredLightSources = new HashMap<>();
    private static final Map<EntityType<?>, ToIntFunction<Entity>> EntityLightHandlers = new HashMap<>();
    private static volatile Snapshot SnapshotLightSources = Snapshot.EMPTY;
    private static Map<Object, LightSource> LastFrameLightSources = new HashMap<>();
    private static ClientLevel lastLevel;

    public static void addLightSources(Vec3 start, Vec3 end, int light) {
        int count = Math.max(1, (int) (start.distanceTo(end) * 3));
        if (count == 1) {
            addLightSources(start, light);
        } else {
            for (int i = 0; i < count; i++) {
                float progress = (float) i / count;
                addLightSources(start.lerp(end, progress), light);
            }
        }
        addLightSources(end, light);
    }

    public static void addLightSources(Vec3 pos, int light) {
        light = Mth.clamp(light, 0, 15);
        if (light > 0) {
            putLightSource(new AnonymousSource(pos.x, pos.y, pos.z, light), pos, light);
        }
    }

    /// 为移动光源提交稳定 ID。同一 ID 每帧只保留最后一次提交的位置和亮度。
    public static void addLightSource(long sourceId, Vec3 pos, int light) {
        light = Mth.clamp(light, 0, 15);
        if (light > 0) {
            putLightSource(sourceId, pos, light);
        }
    }

    /// 注册持续存在的光源。位置和亮度在每次渲染时读取；注销后自动刷新旧光照。
    public static void registerLightSource(long sourceId, Supplier<Vec3> position, IntSupplier luminance) {
        RegisteredLightSources.put(sourceId, new RegisteredSource(Objects.requireNonNull(position), Objects.requireNonNull(luminance)));
    }

    /// 注册固定位置、固定亮度的光源。
    public static void registerLightSource(long sourceId, Vec3 position, int luminance) {
        Objects.requireNonNull(position);
        int light = Mth.clamp(luminance, 0, 15);
        registerLightSource(sourceId, () -> position, () -> light);
    }

    /// 注销持续光源。
    public static void unregisterLightSource(long sourceId) {
        RegisteredLightSources.remove(sourceId);
    }

    /// 按实体类型注册亮度提供器；返回 0 表示该实体当前不发光。
    @SuppressWarnings("unchecked")
    public static <T extends Entity> void registerEntityLight(EntityType<T> type, ToIntFunction<? super T> luminance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(luminance);
        EntityLightHandlers.put(type, entity -> luminance.applyAsInt((T) entity));
    }

    /// 注销实体类型的亮度提供器。
    public static void unregisterEntityLight(EntityType<?> type) {
        EntityLightHandlers.remove(type);
    }

    /// 清除当前世界的光源状态；实体类型注册不受影响。
    public static void clearWorld() {
        RegisteredLightSources.clear();
        FrameLightSources.clear();
        LastFrameLightSources.clear();
        SnapshotLightSources = Snapshot.EMPTY;
        lastLevel = null;
    }

    private static void putLightSource(Object key, Vec3 pos, int light) {
        int x = Mth.floor(pos.x), y = Mth.floor(pos.y), z = Mth.floor(pos.z);
        FrameLightSources.put(key, new LightSource(SectionPos.asLong(x >> SECTION_BITS, y >> SECTION_BITS, z >> SECTION_BITS), pos.x, pos.y, pos.z, light));
    }

    /// 帧末对比光源并标记需要重建的区块。
    public static void update(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL){
            return;
        }
        ClientLevel level = Minecraft.getInstance().level;
        if (level != lastLevel) {
            if (lastLevel != null) {
                clearWorld();
            }
            lastLevel = level;
        }
        if (level != null) {
            for (Map.Entry<Long, RegisteredSource> entry : RegisteredLightSources.entrySet()) {
                RegisteredSource source = entry.getValue();
                Vec3 pos = source.position().get();
                if (pos != null) {
                    int light = Mth.clamp(source.luminance().getAsInt(), 0, 15);
                    if (light > 0)
                        putLightSource(new RegisteredSourceId(entry.getKey()), pos, light);
                }
            }
            if (!EntityLightHandlers.isEmpty()) {
                for (Entity entity : level.entitiesForRendering()) {
                    ToIntFunction<Entity> handler = EntityLightHandlers.get(entity.getType());
                    if (handler != null) {
                        int light = Mth.clamp(handler.applyAsInt(entity), 0, 15);
                        if (light > 0)
                            putLightSource(new EntitySourceId(entity.getId()), entity.position(), light);
                    }
                }
            }
        }
        LevelRenderer levelRenderer = event.getLevelRenderer();
        Snapshot old = SnapshotLightSources;

        if (FrameLightSources.isEmpty()) {
            // 无光源:恢复全部旧影响区块(移除光源后光照回归 vanilla),清空缓存与快照
            if (old.sources.length != 0) {
                LongOpenHashSet sections = new LongOpenHashSet(old.sources.length * 8);
                for (LightSource s : old.sources) {
                    gatherAffectedSections(s.x, s.y, s.z, sections);
                }
                for (long sec : sections) {
                    levelRenderer.setSectionDirty(SectionPos.x(sec), SectionPos.y(sec), SectionPos.z(sec));
                }
                SnapshotLightSources = Snapshot.EMPTY;
                LastFrameLightSources.clear();
            }
            return;
        }

        if (FrameLightSources.equals(LastFrameLightSources)) {
            FrameLightSources.clear();
            return;
        }

        LightSource[] sources = FrameLightSources.values().toArray(new LightSource[0]);

        // 空间查找:按 sectionKey 排序 + 起始索引(同 key 连续,一次哈希定位区间)
        Arrays.sort(sources, Comparator.comparingLong(LightSource::sectionKey));
        Long2IntOpenHashMap startIndex = new Long2IntOpenHashMap(sources.length * 2 + 1);
        startIndex.defaultReturnValue(-1);
        long prevKey = sources[0].sectionKey();
        startIndex.put(prevKey, 0);
        for (int i = 1; i < sources.length; i++) {
            long key = sources[i].sectionKey();
            if (key != prevKey) {
                startIndex.put(key, i);
                prevKey = key;
            }
        }

        // 移动时同时刷新旧位置和新位置，避免旧光照残留。
        LongOpenHashSet dirty = new LongOpenHashSet();
        for (Map.Entry<Object, LightSource> entry : FrameLightSources.entrySet()) {
            LightSource current = entry.getValue();
            LightSource previous = LastFrameLightSources.get(entry.getKey());
            if (!current.equals(previous)) {
                gatherAffectedSections(current.x, current.y, current.z, dirty);
                if (previous != null) {
                    gatherAffectedSections(previous.x, previous.y, previous.z, dirty);
                }
            }
        }
        for (Map.Entry<Object, LightSource> entry : LastFrameLightSources.entrySet()) {
            if (!FrameLightSources.containsKey(entry.getKey())) {
                LightSource removed = entry.getValue();
                gatherAffectedSections(removed.x, removed.y, removed.z, dirty);
            }
        }
        for (long sec : dirty) {
            levelRenderer.setSectionDirty(SectionPos.x(sec), SectionPos.y(sec), SectionPos.z(sec));
        }

        SnapshotLightSources = new Snapshot(sources, startIndex);
        LastFrameLightSources = new HashMap<>(FrameLightSources);
        FrameLightSources.clear();
    }

    /// 方块路径。
    public static int getDynamicLight(BlockAndTintGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        if (state.isSolidRender(level, blockPos)) {
            return originalLight;
        }
        double dynamicLight = getDynamicLightLevel(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
        if (dynamicLight > 0) {
            int blockLevel = LightTexture.block(originalLight);
            if (dynamicLight > blockLevel) {
                return withDynamicLight(originalLight, dynamicLight);
            }
        }
        return originalLight;
    }

    /// 实体路径。
    public static int getDynamicLight(Vec3 eyePos, int originalLight) {
        double dynamicLight = getDynamicLightLevel(eyePos.x, eyePos.y, eyePos.z);
        if (dynamicLight > 0) {
            int blockLevel = LightTexture.block(originalLight);
            if (dynamicLight > blockLevel) {
                return withDynamicLight(originalLight, dynamicLight);
            }
        }
        return originalLight;
    }

    /// 查询连续坐标处的动态光照。
    private static double getDynamicLightLevel(double x, double y, double z) {
        Snapshot snap = SnapshotLightSources;
        if (snap.sources.length == 0) {
            return 0;
        }
        double result = 0;
        int fx = Mth.floor(x), fy = Mth.floor(y), fz = Mth.floor(z);
        int cx = fx >> SECTION_BITS, cy = fy >> SECTION_BITS, cz = fz >> SECTION_BITS;
        double ox = x - cx * 16.0, oy = y - cy * 16.0, oz = z - cz * 16.0;
        int x0 = ox < RADIUS ? cx - 1 : cx;
        int x1 = ox > 16.0 - RADIUS ? cx + 1 : cx;
        int y0 = oy < RADIUS ? cy - 1 : cy;
        int y1 = oy > 16.0 - RADIUS ? cy + 1 : cy;
        int z0 = oz < RADIUS ? cz - 1 : cz;
        int z1 = oz > 16.0 - RADIUS ? cz + 1 : cz;
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    int start = snap.startIndex.get(SectionPos.asLong(bx, by, bz));
                    if (start < 0) {
                        continue;
                    }
                    LightSource[] sources = snap.sources;
                    long key = sources[start].sectionKey();
                    for (int i = start; i < sources.length && sources[i].sectionKey() == key; i++) {
                        LightSource s = sources[i];
                        double ddx = x - s.x;
                        double ddy = y - s.y;
                        double ddz = z - s.z;
                        double distSq = ddx * ddx + ddy * ddy + ddz * ddz;
                        if (distSq <= MAX_RADIUS_SQUARED) {
                            double light = s.luminance * (1.0 - Math.sqrt(distSq) / RADIUS);
                            if (light > result) {
                                result = light;
                            }
                        }
                    }
                }
            }
        }
        return Mth.clamp(result, 0.0, 15.0);
    }

    /// 将动态光照写入 block 光通道。
    private static int withDynamicLight(int originalLight, double dynamicLight) {
        int luminance = (int) (dynamicLight * 16.0);
        return (originalLight & 0xfff00000) | (luminance & 0x000fffff);
    }

    /// 只标记与光源半径相交的区块，查询和刷新共用同一半径。
    private static void gatherAffectedSections(double x, double y, double z, LongOpenHashSet out) {
        int cx = SectionPos.blockToSectionCoord(x);
        int cy = SectionPos.blockToSectionCoord(y);
        int cz = SectionPos.blockToSectionCoord(z);
        for (int sx = cx - 1; sx <= cx + 1; sx++) {
            for (int sy = cy - 1; sy <= cy + 1; sy++) {
                for (int sz = cz - 1; sz <= cz + 1; sz++) {
                    double dx = Math.max(Math.max(sx * 16.0 - x, 0), x - (sx + 1) * 16.0);
                    double dy = Math.max(Math.max(sy * 16.0 - y, 0), y - (sy + 1) * 16.0);
                    double dz = Math.max(Math.max(sz * 16.0 - z, 0), z - (sz + 1) * 16.0);
                    if (dx * dx + dy * dy + dz * dz <= MAX_RADIUS_SQUARED) {
                        out.add(SectionPos.asLong(sx, sy, sz));
                    }
                }
            }
        }
    }

    private record AnonymousSource(double x, double y, double z, int luminance) {}

    private record RegisteredSource(Supplier<Vec3> position, IntSupplier luminance) {}

    private record RegisteredSourceId(long id) {}

    private record EntitySourceId(int id) {}

    private record LightSource(long sectionKey, double x, double y, double z, int luminance) {}

    private record Snapshot(LightSource[] sources, Long2IntOpenHashMap startIndex) {
        static final Snapshot EMPTY = new Snapshot(new LightSource[0], emptyIndex());

        private static Long2IntOpenHashMap emptyIndex() {
            Long2IntOpenHashMap map = new Long2IntOpenHashMap(1);
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
