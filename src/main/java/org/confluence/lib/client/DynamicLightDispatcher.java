package org.confluence.lib.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.mixin.chunk.LevelRendererAccessor;

import java.util.ArrayList;
import java.util.List;

public final class DynamicLightDispatcher {

    public static final DynamicLightDispatcher INSTANCE = new DynamicLightDispatcher(7.75);

    private final double maxRadius;
    private final double maxRadiusSquared;
    private final double falloff;
    private ObjectOpenHashSet<LightSource> current;
    private ObjectOpenHashSet<LightSource> previous;
    private final LongOpenHashSet dirtySections;
    private final Long2ObjectOpenHashMap<LightSource[]> emptySnapshot;
    private volatile Long2ObjectOpenHashMap<LightSource[]> snapshot;
    private boolean changed;

    private DynamicLightDispatcher(double maxRadius) {
        this.maxRadius = maxRadius;
        this.maxRadiusSquared = maxRadius * maxRadius;
        this.falloff = 15.0 / maxRadius;
        this.current = new ObjectOpenHashSet<>();
        this.previous = new ObjectOpenHashSet<>();
        this.dirtySections = new LongOpenHashSet();
        this.emptySnapshot = new Long2ObjectOpenHashMap<>();
        this.snapshot = emptySnapshot;
    }

    public void addLightSource(LightSource source) {
        if (source.luminance() > 0 && current.add(source) && !previous.contains(source)) {
            markImpactSections(source);
            changed = true;
        }
    }

    public void update(LevelRendererAccessor levelRenderer) {
        for (LightSource light : previous) {
            if (!current.contains(light)) {
                markImpactSections(light);
                changed = true;
            }
        }
        ObjectOpenHashSet<LightSource> swap = previous;
        previous = current;
        current = swap;
        current.clear();
        flushDirtySections(levelRenderer);
        if (changed) {
            snapshot = buildSnapshot();
            changed = false;
        }
    }

    public int getDynamicLight(BlockAndTintGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        if (snapshot.isEmpty()) {
            return originalLight;
        }
        int light = originalLight;
        if (!state.isSolidRender(level, blockPos)) {
            double dynamicLight = getDynamicLightLevel(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            if (dynamicLight > LightTexture.block(originalLight)) {
                light = withDynamicLight(originalLight, dynamicLight);
            }
        }
        return light;
    }

    public int getDynamicLight(Vec3 eyePos, int originalLight) {
        int light = originalLight;
        double dynamicLight = getDynamicLightLevel(eyePos.x, eyePos.y, eyePos.z);
        if (dynamicLight > LightTexture.block(originalLight)) {
            light = withDynamicLight(originalLight, dynamicLight);
        }
        return light;
    }

    private double getDynamicLightLevel(double x, double y, double z) {
        Long2ObjectOpenHashMap<LightSource[]> sources = snapshot;
        if (sources.isEmpty()) {
            return 0;
        }
        double result = 0;
        int sectionX = Mth.floor(x) >> 4, sectionY = Mth.floor(y) >> 4, sectionZ = Mth.floor(z) >> 4;
        // section 内偏移（含小数）：半径 < 16 格，只有偏移落在 ±maxRadius 之内的相邻 section 才可能照到这里
        double offsetX = x - (sectionX << 4), offsetY = y - (sectionY << 4), offsetZ = z - (sectionZ << 4);
        int x0 = offsetX <= maxRadius ? sectionX - 1 : sectionX;
        int x1 = offsetX >= 16.0 - maxRadius ? sectionX + 1 : sectionX;
        int y0 = offsetY <= maxRadius ? sectionY - 1 : sectionY;
        int y1 = offsetY >= 16.0 - maxRadius ? sectionY + 1 : sectionY;
        int z0 = offsetZ <= maxRadius ? sectionZ - 1 : sectionZ;
        int z1 = offsetZ >= 16.0 - maxRadius ? sectionZ + 1 : sectionZ;
        for (int bx = x0; bx <= x1; bx++) {
            for (int by = y0; by <= y1; by++) {
                for (int bz = z0; bz <= z1; bz++) {
                    LightSource[] group = sources.get(SectionPos.asLong(bx, by, bz));
                    if (group != null) {
                        for (LightSource lightSource : group) {
                            Vec3 pos = lightSource.position();
                            double dx = x - pos.x;
                            double dy = y - pos.y;
                            double dz = z - pos.z;
                            double distSq = dx * dx + dy * dy + dz * dz;
                            if (distSq <= maxRadiusSquared) { // 超出半径时光值为负，直接跳过
                                double light = lightSource.luminance() - Math.sqrt(distSq) * falloff;
                                if (light > result) {
                                    result = light;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Mth.clamp(result, 0.0, 15.0);
    }

    private Long2ObjectOpenHashMap<LightSource[]> buildSnapshot() {
        if (previous.isEmpty()) {
            return emptySnapshot;
        }
        Long2ObjectOpenHashMap<List<LightSource>> groups = new Long2ObjectOpenHashMap<>();
        for (LightSource light : previous) {
            groups.computeIfAbsent(sectionOf(light), s -> new ArrayList<>()).add(light);
        }
        Long2ObjectOpenHashMap<LightSource[]> next = new Long2ObjectOpenHashMap<>(groups.size() * 2);
        for (Long2ObjectMap.Entry<List<LightSource>> group : groups.long2ObjectEntrySet()) {
            next.put(group.getLongKey(), group.getValue().toArray(new LightSource[0]));
        }
        return next;
    }

    private void markImpactSections(LightSource source) {
        Vec3 position = source.position();
        double radius = Mth.clamp(source.luminance(), 0, 15) / falloff;
        int minX = SectionPos.blockToSectionCoord(position.x() - radius);
        int maxX = SectionPos.blockToSectionCoord(position.x() + radius);
        int minY = SectionPos.blockToSectionCoord(position.y() - radius);
        int maxY = SectionPos.blockToSectionCoord(position.y() + radius);
        int minZ = SectionPos.blockToSectionCoord(position.z() - radius);
        int maxZ = SectionPos.blockToSectionCoord(position.z() + radius);
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cy = minY; cy <= maxY; cy++) {
                for (int cz = minZ; cz <= maxZ; cz++) {
                    dirtySections.add(SectionPos.asLong(cx, cy, cz));
                }
            }
        }
    }

    private void flushDirtySections(LevelRendererAccessor levelRenderer) {
        for (LongIterator it = dirtySections.iterator(); it.hasNext(); ) {
            long section = it.nextLong();
            levelRenderer.callSetSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section), false);
        }
        dirtySections.clear();
    }

    private static long sectionOf(LightSource source) {
        Vec3 pos = source.position();
        return SectionPos.asLong(SectionPos.blockToSectionCoord(pos.x()), SectionPos.blockToSectionCoord(pos.y()), SectionPos.blockToSectionCoord(pos.z()));
    }

    private static int withDynamicLight(int originalLight, double dynamicLight) {
        int smooth = (int) (dynamicLight * 16.0) + 8;
        return (originalLight & 0xfff00000) | smooth;
    }

    public static final class LightSource {

        private final Vec3 position;
        private final int luminance;
        private final int hash;

        public LightSource(Vec3 position, int luminance) {
            this.position = position;
            this.luminance = luminance;
            this.hash = Long.hashCode(Double.doubleToLongBits(position.x) * 0x9E3779B97F4A7C15L ^ Double.doubleToLongBits(position.y) * 0xC2B2AE3D27D4EB4FL ^ Double.doubleToLongBits(position.z) * 0x165667B19E3779F9L ^ luminance * 0x27D4EB2F165667C5L);
        }

        public Vec3 position() {
            return position;
        }

        public int luminance() {
            return luminance;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (object instanceof LightSource other) {
                return luminance == other.luminance && position.x == other.position.x && position.y == other.position.y && position.z == other.position.z;
            }
            return false;
        }
    }
}
