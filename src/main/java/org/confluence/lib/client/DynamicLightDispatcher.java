package org.confluence.lib.client;

import net.minecraft.client.renderer.LevelRenderer;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 动态光照调度器。
 */
@SuppressWarnings("unused")
public final class DynamicLightDispatcher {
    private static final double MAX_RADIUS_SQUARED = 7.75 * 7.75;
    private static final double FALLOFF = 15.0 / 7.75;
    private static final int SECTION_BITS = 4;
    private static final ArrayList<LightSource> LightSources = new ArrayList<>();
    private static volatile Snapshot SnapshotLightSources = Snapshot.EMPTY;
    private static Long2ObjectOpenHashMap<LightSource> LastFrameLightSources = new Long2ObjectOpenHashMap<>();

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
            int x = Mth.floor(pos.x), y = Mth.floor(pos.y), z = Mth.floor(pos.z);
            LightSources.add(new LightSource(lightId(pos.x, pos.y, pos.z, light), SectionPos.asLong(x >> SECTION_BITS, y >> SECTION_BITS, z >> SECTION_BITS), pos.x, pos.y, pos.z, light));
        }
    }

    /**
     * 帧末调度
     */
    public static void update(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL){
            return;
        }
        LevelRenderer levelRenderer = event.getLevelRenderer();
        Snapshot old = SnapshotLightSources;

        if (LightSources.isEmpty()) {
            // 无光源:恢复全部旧影响区块(移除光源后光照回归 vanilla),清空缓存与快照
            if (old.sources.length != 0) {
                LongOpenHashSet sections = new LongOpenHashSet(old.sources.length * 8);
                for (LightSource s : old.sources) {
                    gatherClosestChunks(s.x, s.y, s.z, sections);
                }
                for (long sec : sections) {
                    levelRenderer.setSectionDirty(SectionPos.x(sec), SectionPos.y(sec), SectionPos.z(sec));
                }
                SnapshotLightSources = Snapshot.EMPTY;
                LastFrameLightSources.clear();
            }
            return;
        }

        // 静止帧短路:光源与上帧完全一致(id 全部命中且亮度相同、数量相同)
        // → 快照排序数组、跨帧缓存、区块均无变化,直接复用
        boolean allStatic = LightSources.size() == LastFrameLightSources.size();
        if (allStatic) {
            for (LightSource s : LightSources) {
                LightSource matched = LastFrameLightSources.get(s.id());
                if (matched == null || matched.luminance != s.luminance) {
                    allStatic = false;
                    break;
                }
            }
        }
        if (allStatic) {
            LightSources.clear();
            return;
        }

        LightSource[] sources = LightSources.toArray(new LightSource[0]);
        LightSources.clear();

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

        // 跨帧光源对比:未命中 = 新增/大幅移动;命中但位置或亮度不同 = 移动;命中且相同 = 静止
        Long2ObjectOpenHashMap<LightSource> newCache = new Long2ObjectOpenHashMap<>(sources.length * 2);
        LongOpenHashSet dirty = new LongOpenHashSet();
        for (LightSource s : sources) {
            LightSource matched = LastFrameLightSources.get(s.id());
            if (matched == null || matched.x != s.x || matched.y != s.y || matched.z != s.z || matched.luminance != s.luminance) {
                gatherClosestChunks(s.x, s.y, s.z, dirty);
            }
            newCache.put(s.id(), s);
        }
        // 旧缓存未被命中的光源 = 移除 → 刷新其影响区块
        for (Long2ObjectMap.Entry<LightSource> entry : LastFrameLightSources.long2ObjectEntrySet()) {
            if (!newCache.containsKey(entry.getLongKey())) {
                LightSource removed = entry.getValue();
                gatherClosestChunks(removed.x, removed.y, removed.z, dirty);
            }
        }
        for (long sec : dirty) {
            levelRenderer.setSectionDirty(SectionPos.x(sec), SectionPos.y(sec), SectionPos.z(sec));
        }

        // 快照与跨帧缓存引用替换
        SnapshotLightSources = new Snapshot(sources, startIndex);
        LastFrameLightSources = newCache;
    }

    /**
     * 方块路径
     */
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

    /**
     * 实体路径
     */
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

    /**
     * 查询 (x, y, z) 连续坐标处的动态光照。
     */
    private static double getDynamicLightLevel(double x, double y, double z) {
        Snapshot snap = SnapshotLightSources;
        if (snap.sources.length == 0) {
            return 0;
        }
        double result = 0;
        int fx = Mth.floor(x), fy = Mth.floor(y), fz = Mth.floor(z);
        int cx = fx >> SECTION_BITS, cy = fy >> SECTION_BITS, cz = fz >> SECTION_BITS;
        int ox = fx & 15, oy = fy & 15, oz = fz & 15;
        int x0 = ox <= 6 ? cx - 1 : cx;
        int x1 = ox >= 9 ? cx + 1 : cx;
        int y0 = oy <= 6 ? cy - 1 : cy;
        int y1 = oy >= 9 ? cy + 1 : cy;
        int z0 = oz <= 6 ? cz - 1 : cz;
        int z1 = oz >= 9 ? cz + 1 : cz;
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
                            double light = s.luminance - Math.sqrt(distSq) * FALLOFF;
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

    /**
     * 将动态光照写入 block 光通道。
     */
    private static int withDynamicLight(int originalLight, double dynamicLight) {
        int luminance = (int) (dynamicLight * 16.0);
        return (originalLight & 0xfff00000) | (luminance & 0x000fffff);
    }

    /** 写入光源影响区块*/
    private static void gatherClosestChunks(double x, double y, double z, LongOpenHashSet out) {
        int cx = SectionPos.blockToSectionCoord(x);
        int cy = SectionPos.blockToSectionCoord(y);
        int cz = SectionPos.blockToSectionCoord(z);
        out.add(SectionPos.asLong(cx, cy, cz));
        int sx = (Mth.floor(x) & 15) >= 8 ? 1 : -1;
        int sy = (Mth.floor(y) & 15) >= 8 ? 1 : -1;
        int sz = (Mth.floor(z) & 15) >= 8 ? 1 : -1;
        for (int i = 0; i < 7; i++) {
            if (i % 4 == 0) {
                cx += sx;
            } else if (i % 4 == 1) {
                cz += sz;
            } else if (i % 4 == 2) {
                cx -= sx;
            } else {
                cz -= sz;
                cy += sy;
            }
            out.add(SectionPos.asLong(cx, cy, cz));
        }
    }

    /** 光源身份哈希:位置亮度混合计算*/
    private static long lightId(double x, double y, double z, int luminance) {
        long h = Double.doubleToRawLongBits(x) * 0x9E3779B97F4A7C15L ^ Double.doubleToRawLongBits(y) * 0xBF58476D1CE4E5B9L ^ Double.doubleToRawLongBits(z) * 0x94D049BB133111EBL ^ (long) luminance * 0x27D4EB2F165667C5L;
        h ^= h >>> 32;
        return h;
    }

    private record LightSource(long id, long sectionKey, double x, double y, double z, int luminance) {}

    private record Snapshot(LightSource[] sources, Long2IntOpenHashMap startIndex) {
        static final Snapshot EMPTY = new Snapshot(new LightSource[0], emptyIndex());

        private static Long2IntOpenHashMap emptyIndex() {
            Long2IntOpenHashMap map = new Long2IntOpenHashMap(1);
            map.defaultReturnValue(-1);
            return map;
        }
    }
}
