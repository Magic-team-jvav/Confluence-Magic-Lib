package org.confluence.lib.client;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.client.Camera;
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
import org.confluence.lib.client.light.DynamicLightEffects;
import org.confluence.lib.client.light.DynamicLightGpu;
import org.joml.Matrix4f;

import java.util.*;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

public final class DynamicLightDispatcher {

    public static final int MAX_STRENGTH = 0xFF;

    public static final DynamicLightDispatcher INSTANCE = new DynamicLightDispatcher(7.75);

    private final double maxRadius;
    private final double maxRadiusSquared;
    private final double falloff;
    private ObjectOpenHashSet<LightSource> current;
    private ObjectOpenHashSet<LightSource> previous;
    private final List<LightSource> collectedSources = new ArrayList<>();
    private final LongOpenHashSet dirtySections;
    private final Long2ObjectOpenHashMap<LightSource[]> emptySnapshot;
    private volatile Long2ObjectOpenHashMap<LightSource[]> snapshot;
    private static final Comparator<LightSource> BY_STRENGTH = Comparator.comparingInt(LightSource::strength).reversed();
    private final Long2ObjectOpenHashMap<SnapshotGroup> snapshotGroups = new Long2ObjectOpenHashMap<>();
    private final List<SnapshotGroup> groupPool = new ArrayList<>();
    private boolean changed;
    private ClientLevel frameLevel;
    private boolean lastGpu;

    /// 旧版公开登记入口的兼容提交表，光照计算仍由同一个实例完成。
    private final Map<Long, LightSource> legacyFrameSources = new HashMap<>();
    private final Map<Long, RegisteredSource> legacyRegisteredSources = new HashMap<>();
    private ClientLevel legacyLevel;
    private static final Map<EntityType<?>, ToIntFunction<Entity>> EntityLightHandlers = new HashMap<>();

    private DynamicLightDispatcher(double maxRadius) {
        this.maxRadius = maxRadius;
        this.maxRadiusSquared = maxRadius * maxRadius;
        this.falloff = MAX_STRENGTH / maxRadius;
        this.current = new ObjectOpenHashSet<>(16, 0.5F);
        this.previous = new ObjectOpenHashSet<>(16, 0.5F);
        this.dirtySections = new LongOpenHashSet();
        this.emptySnapshot = new Long2ObjectOpenHashMap<>();
        this.snapshot = emptySnapshot;
    }

    public void addLightSource(Vec3 position, int strength) {
        if (strength > 0) add(new LightSource(position, strength));
    }

    public void addLightSource(Vec3 position, float strength) {
        addLightSource(position, Math.round(strength * MAX_STRENGTH));
    }

    public void addLightSource(LightSource source) {
        add(source);
    }

    private void add(LightSource source) {
        if (source == null || source.strength() <= 0
                || !Double.isFinite(source.position().x) || !Double.isFinite(source.position().y)
                || !Double.isFinite(source.position().z) || !current.add(source)) return;
        boolean gpu = DynamicLightGpu.terrainGpu();
        if (changed) {
            collectedSources.add(source);
            if (!gpu && !previous.contains(source)) markImpactSections(source);
        } else if (!previous.contains(source)) {
            // 第一次变化时补齐此前采集的光源；完全静止的帧不维护额外列表。
            collectedSources.addAll(current);
            changed = true;
            if (!gpu) markImpactSections(source);
        }
    }

    /// 兼容原有整数光级入口，0～15 光级换算为原生 0～255 强度。
    public static void addLightSources(Vec3 position, int luminance) {
        int light = Mth.clamp(luminance, 0, 15);
        if (light > 0) {
            INSTANCE.addLightSource(position, light * 17);
        }
    }

    /// 兼容原有线段光源入口，沿用原有采样间隔。
    public static void addLightSources(Vec3 start, Vec3 end, int luminance) {
        int count = Math.max(1, (int) (start.distanceTo(end) * 3));
        if (count == 1) {
            addLightSources(start, luminance);
        } else {
            for (int i = 0; i < count; i++) {
                float progress = (float) i / count;
                addLightSources(start.lerp(end, progress), luminance);
            }
        }
        addLightSources(end, luminance);
    }

    /// 同一 ID 保留本帧最后一次正亮度提交，零亮度沿用原有忽略语义。
    public static void addLightSource(long sourceId, Vec3 position, int luminance) {
        int light = Mth.clamp(luminance, 0, 15);
        if (light > 0) {
            INSTANCE.legacyFrameSources.put(sourceId, new LightSource(position, light * 17));
        }
    }

    /// 保留原有持续光源登记入口，提供器仍返回 0～15 光级。
    public static void registerLightSource(long sourceId, Supplier<Vec3> position, IntSupplier luminance) {
        INSTANCE.checkLegacyLevel();
        INSTANCE.legacyRegisteredSources.put(sourceId, new RegisteredSource(Objects.requireNonNull(position), Objects.requireNonNull(luminance)));
    }

    /// 保留原有固定位置持续光源入口。
    public static void registerLightSource(long sourceId, Vec3 position, int luminance) {
        Objects.requireNonNull(position);
        int light = Mth.clamp(luminance, 0, 15);
        registerLightSource(sourceId, () -> position, () -> light);
    }

    /// 注销原有持续光源。
    public static void unregisterLightSource(long sourceId) {
        INSTANCE.legacyRegisteredSources.remove(sourceId);
    }

    /// 保留原有实体类型登记入口，提供器仍返回 0～15 光级。
    @SuppressWarnings("unchecked")
    public static <T extends Entity> void registerEntityLight(EntityType<T> type, ToIntFunction<? super T> luminance) {
        Objects.requireNonNull(type);
        Objects.requireNonNull(luminance);
        EntityLightHandlers.put(type, entity -> luminance.applyAsInt((T) entity));
    }

    /// 注销原有实体类型光源提供器。
    public static void unregisterEntityLight(EntityType<?> type) {
        EntityLightHandlers.remove(type);
    }

    /// 保留显式退出世界清理入口，类型提供器不受影响。
    public static void setLevel(ClientLevel level) {
        clearWorld();
        INSTANCE.frameLevel = level;
    }

    public static void clearWorld() {
        INSTANCE.current.clear();
        INSTANCE.collectedSources.clear();
        INSTANCE.previous.clear();
        INSTANCE.dirtySections.clear();
        INSTANCE.snapshot = INSTANCE.emptySnapshot;
        INSTANCE.snapshotGroups.clear();
        INSTANCE.groupPool.clear();
        INSTANCE.changed = false;
        INSTANCE.legacyFrameSources.clear();
        INSTANCE.legacyRegisteredSources.clear();
        INSTANCE.legacyLevel = null;
        INSTANCE.frameLevel = null;
        DynamicLightGpu.clearWorld();
        DynamicLightEffects.clearWorld();
    }

    /// 保留原有静态采样入口。
    public static int getDynamicLight(BlockAndTintGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        return INSTANCE.sampleDynamicLight(level, state, blockPos, originalLight);
    }

    /// 保留原有静态实体采样入口。
    public static int getDynamicLight(Vec3 position, int originalLight) {
        return INSTANCE.sampleDynamicLight(position, originalLight);
    }

    /// 换世界时仅清理旧持续登记，不触碰本帧已提交的实例光源。
    private void checkLegacyLevel() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != legacyLevel) {
            if (legacyLevel != null) {
                legacyRegisteredSources.clear();
            }
            legacyLevel = level;
        }
    }

    /// 兼容入口只向原生本帧集合提交，不维护独立的光照快照。
    private void collectLegacySources() {
        checkLegacyLevel();
        if (legacyLevel != null) {
            for (RegisteredSource source : legacyRegisteredSources.values()) {
                Vec3 position = source.position().get();
                if (position != null) {
                    addLightSources(position, source.luminance().getAsInt());
                }
            }
            if (!EntityLightHandlers.isEmpty()) {
                for (Entity entity : legacyLevel.entitiesForRendering()) {
                    ToIntFunction<Entity> handler = EntityLightHandlers.get(entity.getType());
                    if (handler != null) {
                        int light = Mth.clamp(handler.applyAsInt(entity), 0, 15);
                        if (light > 0) {
                            addLightSource(entity.position(), light * 17);
                        }
                    }
                }
            }
        }
        for (LightSource source : legacyFrameSources.values()) {
            addLightSource(source);
        }
        legacyFrameSources.clear();
    }

    private record RegisteredSource(Supplier<Vec3> position, IntSupplier luminance) {}

    public void beginFrame(LevelRenderer renderer, Camera camera, Matrix4f view, float partialTick) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != frameLevel) {
            clearWorld();
            frameLevel = level;
        }
        DynamicLightGpu.prepare();
        DynamicLightRegister.collect(partialTick);
        update(renderer);
        DynamicLightGpu.begin(snapshot, camera.getPosition(), view);
        if (lastGpu && !DynamicLightGpu.terrainGpu()) {
            previous.forEach(this::markImpactSections);
            flushDirtySections(renderer);
            lastGpu = false;
        }
    }

    public void update(LevelRenderer levelRenderer) {
        collectLegacySources();
        boolean gpu = DynamicLightGpu.terrainGpu();
        if (gpu) {
            // 新增已检测过；无新增且数量相同意味着集合未变，不必再反向查找删除项。
            changed |= current.size() != previous.size();
        } else {
            for (LightSource light : previous) {
                if (!current.contains(light)) {
                    markImpactSections(light);
                    changed = true;
                }
            }
        }
        if (gpu != lastGpu) {
            // 后端切换时清除旧网格内烘焙的动态光，仅切换这一帧需要刷新。
            previous.forEach(this::markImpactSections);
            current.forEach(this::markImpactSections);
            lastGpu = gpu;
        }
        // 只有删除、没有新增的帧也要收集剩余光源。
        if (changed && collectedSources.isEmpty()) collectedSources.addAll(current);
        ObjectOpenHashSet<LightSource> swap = previous;
        previous = current;
        current = swap;
        current.clear();
        if (changed) {
            snapshot = buildSnapshot();
            changed = false;
        }
        collectedSources.clear();
        flushDirtySections(levelRenderer);
    }

    private int sampleDynamicLight(BlockAndTintGetter level, BlockState state, BlockPos blockPos, int originalLight) {
        if (snapshot.isEmpty()) {
            return originalLight;
        }
        int light = originalLight;
        if (!state.isSolidRender(level, blockPos)) {
            double strength = getDynamicLightLevel(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5);
            if (strength > LightTexture.block(originalLight) * 17) { // 原版光照是 0~15 级，换算到同一尺度再比
                light = withDynamicLight(originalLight, strength);
            }
        }
        return light;
    }

    private int sampleDynamicLight(Vec3 eyePos, int originalLight) {
        int light = originalLight;
        double strength = getDynamicLightLevel(eyePos.x, eyePos.y, eyePos.z);
        if (strength > LightTexture.block(originalLight) * 17) {
            light = withDynamicLight(originalLight, strength);
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
                                double strength = lightSource.strength() - Math.sqrt(distSq) * falloff;
                                if (strength > result) {
                                    result = strength;
                                }
                            }
                        }
                    }
                }
            }
        }
        return Mth.clamp(result, 0.0, MAX_STRENGTH);
    }

    private Long2ObjectOpenHashMap<LightSource[]> buildSnapshot() {
        if (previous.isEmpty()) {
            return emptySnapshot;
        }
        snapshotGroups.clear();
        int used = 0;
        long lastSection = 0;
        SnapshotGroup lastGroup = null;
        // 按采集顺序顺序读取，避免扫描哈希表；相邻同组光源复用查找结果。
        for (LightSource light : collectedSources) {
            long section = sectionOf(light);
            SnapshotGroup group = lastGroup != null && section == lastSection ? lastGroup : snapshotGroups.get(section);
            if (group == null) {
                if (used == groupPool.size()) groupPool.add(new SnapshotGroup());
                group = groupPool.get(used++);
                snapshotGroups.put(section, group);
            }
            group.add(light);
            lastSection = section;
            lastGroup = group;
        }
        Long2ObjectOpenHashMap<LightSource[]> next = new Long2ObjectOpenHashMap<>(snapshotGroups.size());
        for (var iterator = snapshotGroups.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            Long2ObjectMap.Entry<SnapshotGroup> entry = iterator.next();
            SnapshotGroup group = entry.getValue();
            // 等强度光源不排序；只复用构建缓冲，发布后的快照与数组保持不可变。
            if (group.mixedStrength) group.lights.sort(BY_STRENGTH);
            next.put(entry.getLongKey(), group.lights.toArray(new LightSource[group.lights.size()]));
            group.lights.clear();
            group.mixedStrength = false;
        }
        return next;
    }

    private static final class SnapshotGroup {
        private final List<LightSource> lights = new ArrayList<>();
        private boolean mixedStrength;

        private void add(LightSource light) {
            if (!lights.isEmpty() && lights.get(0).strength() != light.strength())
                mixedStrength = true;
            lights.add(light);
        }
    }

    private void markImpactSections(LightSource source) {
        Vec3 position = source.position();
        double radius = Mth.clamp(source.strength(), 0, MAX_STRENGTH) / falloff + 1.0;
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

    private void flushDirtySections(LevelRenderer levelRenderer) {
        for (LongIterator it = dirtySections.iterator(); it.hasNext(); ) {
            long section = it.nextLong();
            levelRenderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
        }
        dirtySections.clear();
    }

    private static long sectionOf(LightSource source) {
        Vec3 pos = source.position();
        return SectionPos.asLong(SectionPos.blockToSectionCoord(pos.x()), SectionPos.blockToSectionCoord(pos.y()), SectionPos.blockToSectionCoord(pos.z()));
    }

    private static int withDynamicLight(int originalLight, double strength) {
        // 内部强度为 0～255，输出时才转换到原版 lightmap 的 0～240 坐标。
        int smooth = Mth.clamp((int) (strength * (16.0 / 17.0)) + 8, 8, 248);
        smooth = Math.max(originalLight & 0xFF, smooth);
        return (originalLight & 0xfff00000) | smooth;
    }

    public static final class LightSource {

        private final Vec3 position;
        private final int strength;
        private final int hash;

        public LightSource(Vec3 position, int strength) {
            this.position = position;
            this.strength = Mth.clamp(strength, 0, MAX_STRENGTH);
            // 哈希使用裁剪后的亮度，并将 -0.0 归一为 +0.0，与 equals 保持一致。
            this.hash = Long.hashCode(Double.doubleToLongBits(position.x + 0.0) * 0x9E3779B97F4A7C15L ^ Double.doubleToLongBits(position.y + 0.0) * 0xC2B2AE3D27D4EB4FL ^ Double.doubleToLongBits(position.z + 0.0) * 0x165667B19E3779F9L ^ this.strength * 0x27D4EB2F165667C5L);
        }

        public Vec3 position() {
            return position;
        }

        public int strength() {
            return strength;
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
                return strength == other.strength && position.x == other.position.x && position.y == other.position.y && position.z == other.position.z;
            }
            return false;
        }
    }
}
