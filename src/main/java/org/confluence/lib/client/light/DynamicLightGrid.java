package org.confluence.lib.client.light;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.DynamicLightDispatcher.LightSource;

import java.nio.FloatBuffer;
import java.util.Comparator;

// 按受光 section 分组：像素只需查询一个桶，跨界光源由 CPU 放入所有受影响的组。
public final class DynamicLightGrid {
    private static final Comparator<LightSource> BY_STRENGTH = Comparator.comparingInt(LightSource::strength).reversed();
    private final Long2ObjectOpenHashMap<ObjectArrayList<LightSource>> groups = new Long2ObjectOpenHashMap<>();
    private final ObjectArrayList<ObjectArrayList<LightSource>> groupPool = new ObjectArrayList<>();
    private final ObjectArrayList<LightSource> visible = new ObjectArrayList<>();
    private Long2ObjectMap<LightSource[]> collectedSources;
    private Vec3 collectedCamera;
    private double collectedDistance;
    private double reuseRadiusSquared;
    private int size;
    private int buckets;

    // 快照不变时，三角不等式保证此移动范围内没有光源跨越可见边界。
    public boolean canReuse(Long2ObjectMap<LightSource[]> sources, Vec3 camera, double distance) {
        return sources == collectedSources && distance == collectedDistance && collectedCamera != null
                && (camera.equals(collectedCamera) || camera.distanceToSqr(collectedCamera) < reuseRadiusSquared);
    }

    public void collect(Long2ObjectMap<LightSource[]> sources, Vec3 camera, double distance) {
        for (ObjectArrayList<LightSource> group : groups.values()) group.clear();
        groups.clear();
        visible.clear();
        size = 0;
        double radius = distance + 7.75;
        double farthestIncluded = 0.0;
        double nearestExcluded = Double.POSITIVE_INFINITY;
        // 可见区块按水平范围加载；球形距离会漏掉角落及同列高处的光源。
        for (var iterator = Long2ObjectMaps.fastIterator(sources); iterator.hasNext(); ) {
            var entry = iterator.next();
            for (LightSource source : entry.getValue()) {
                Vec3 position = source.position();
                double separation = Math.max(Math.abs(position.x - camera.x), Math.abs(position.z - camera.z));
                if (separation <= radius) {
                    visible.add(source);
                    farthestIncluded = Math.max(farthestIncluded, separation);
                } else {
                    nearestExcluded = Math.min(nearestExcluded, separation);
                }
            }
        }
        // 全局排一次序，分配后的每个组自然保持亮度降序。
        visible.sort(BY_STRENGTH);
        for (LightSource source : visible) {
            Vec3 p = source.position();
            double reach = source.strength() * (7.75 / 255.0) + 1.0E-4;
            int minX = SectionPos.blockToSectionCoord(p.x - reach), maxX = SectionPos.blockToSectionCoord(p.x + reach);
            int minY = SectionPos.blockToSectionCoord(p.y - reach), maxY = SectionPos.blockToSectionCoord(p.y + reach);
            int minZ = SectionPos.blockToSectionCoord(p.z - reach), maxZ = SectionPos.blockToSectionCoord(p.z + reach);
            for (int x = minX; x <= maxX; x++)
                for (int y = minY; y <= maxY; y++)
                    for (int z = minZ; z <= maxZ; z++) {
                        double dx = Math.max(Math.max(x * 16.0 - p.x, p.x - (x * 16.0 + 16.0)), 0.0);
                        double dy = Math.max(Math.max(y * 16.0 - p.y, p.y - (y * 16.0 + 16.0)), 0.0);
                        double dz = Math.max(Math.max(z * 16.0 - p.z, p.z - (z * 16.0 + 16.0)), 0.0);
                        if (dx * dx + dy * dy + dz * dz > reach * reach) continue;
                        long key = SectionPos.asLong(x, y, z);
                        ObjectArrayList<LightSource> group = groups.get(key);
                        if (group == null) {
                            int index = groups.size();
                            if (index == groupPool.size()) groupPool.add(new ObjectArrayList<>());
                            group = groupPool.get(index);
                            groups.put(key, group);
                        }
                        group.add(source);
                        size++;
                    }
        }
        collectedSources = sources;
        collectedCamera = camera;
        collectedDistance = distance;
        double margin = Math.max(0.0, Math.min(radius - farthestIncluded, nearestExcluded - radius) - 1.0E-6);
        reuseRadiusSquared = margin * margin;
        buckets = 1;
        while (buckets < groups.size() * 2) buckets <<= 1;
    }

    public int texels() {return buckets * 2 + size;}

    public int mask() {return size == 0 ? -1 : buckets - 1;}

    public void write(FloatBuffer data, int anchorX, int anchorY, int anchorZ) {
        data.clear();
        // 光源区域随后全部覆盖，只需清空哈希桶。
        for (int i = 0; i < buckets * 8; i++) data.put(i, 0.0F);
        int offset = buckets * 2;
        double originX = anchorX * 16.0, originY = anchorY * 16.0, originZ = anchorZ * 16.0;
        for (var iterator = groups.long2ObjectEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            long key = entry.getLongKey();
            int x = SectionPos.x(key) - anchorX, y = SectionPos.y(key) - anchorY, z = SectionPos.z(key) - anchorZ;
            int slot = hash(x, y, z) & (buckets - 1);
            while (data.get(slot * 8 + 4) != 0.0F) slot = (slot + 1) & (buckets - 1);
            int start = offset;
            for (LightSource source : entry.getValue()) {
                Vec3 p = source.position();
                data.put(offset * 4, (float) (p.x - originX));
                data.put(offset * 4 + 1, (float) (p.y - originY));
                data.put(offset * 4 + 2, (float) (p.z - originZ));
                data.put(offset * 4 + 3, source.strength());
                offset++;
            }
            data.put(slot * 8, x).put(slot * 8 + 1, y).put(slot * 8 + 2, z).put(slot * 8 + 3, start);
            data.put(slot * 8 + 4, offset - start);
            data.put(slot * 8 + 5, data.get(start * 4 + 3));
        }
        data.position(0).limit(texels() * 4);
    }

    public void clear() {
        collectedSources = null;
        collectedCamera = null;
        reuseRadiusSquared = 0.0;
        groups.clear();
        groupPool.clear();
        visible.clear();
        size = 0;
        buckets = 1;
    }

    public static int hash(int x, int y, int z) {
        return x * 73856093 ^ y * 19349663 ^ z * 83492791;
    }
}
