package org.confluence.lib.client.light;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.DynamicLightDispatcher.LightSource;

import java.nio.FloatBuffer;

// GPU 空间哈希表：每个桶两个 RGBA texel，随后存放按 section 分组的光源。
public final class DynamicLightGrid {
    private final Long2LongOpenHashMap groups = new Long2LongOpenHashMap();
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
        groups.clear();
        visible.clear();
        size = 0;
        double radius = distance + 7.75;
        double limit = radius * radius;
        double farthestIncluded = 0.0;
        double nearestExcluded = Double.POSITIVE_INFINITY;
        for (var iterator = Long2ObjectMaps.fastIterator(sources); iterator.hasNext(); ) {
            var entry = iterator.next();
            int start = visible.size();
            LightSource[] lights = entry.getValue();
            boolean fullyVisible = false;
            // 小组直接测距更便宜；较大组才用 section 边界批量判定。
            if (lights.length > 8) {
                long section = entry.getLongKey();
                double dx = Math.abs(SectionPos.x(section) * 16.0 + 8.0 - camera.x);
                double dy = Math.abs(SectionPos.y(section) * 16.0 + 8.0 - camera.y);
                double dz = Math.abs(SectionPos.z(section) * 16.0 + 8.0 - camera.z);
                double farX = dx + 8.0, farY = dy + 8.0, farZ = dz + 8.0;
                double farSquared = farX * farX + farY * farY + farZ * farZ;
                fullyVisible = farSquared <= limit - 1.0E-7;
                if (fullyVisible) farthestIncluded = Math.max(farthestIncluded, farSquared);
                if (!fullyVisible) {
                    double nearX = Math.max(dx - 8.0, 0.0), nearY = Math.max(dy - 8.0, 0.0), nearZ = Math.max(dz - 8.0, 0.0);
                    double nearSquared = nearX * nearX + nearY * nearY + nearZ * nearZ;
                    if (nearSquared > limit + 1.0E-7) {
                        nearestExcluded = Math.min(nearestExcluded, nearSquared);
                        continue;
                    }
                }
            }
            // 整个 section 在视距内时直接复制引用；边界组仍逐个测距。
            if (fullyVisible) {
                visible.addElements(start, lights);
            } else {
                for (LightSource source : lights) {
                    double squared = source.position().distanceToSqr(camera);
                    if (squared <= limit) {
                        visible.add(source);
                        farthestIncluded = Math.max(farthestIncluded, squared);
                    } else {
                        nearestExcluded = Math.min(nearestExcluded, squared);
                    }
                }
            }
            int count = visible.size() - start;
            if (count == 0) continue;
            // 只保存引用和范围，复用快照的分组与排序结果。
            groups.put(entry.getLongKey(), ((long) start << 32) | count);
        }
        collectedSources = sources;
        collectedCamera = camera;
        collectedDistance = distance;
        // 向内留浮点误差余量；边界处不复用，仍按原条件逐个判断。
        double margin = Math.max(0.0, Math.min(radius - Math.sqrt(farthestIncluded), Math.sqrt(nearestExcluded) - radius) - 1.0E-6);
        reuseRadiusSquared = margin * margin;
        size = visible.size();
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
        for (var iterator = groups.long2LongEntrySet().fastIterator(); iterator.hasNext(); ) {
            var entry = iterator.next();
            long key = entry.getLongKey();
            int x = SectionPos.x(key) - anchorX, y = SectionPos.y(key) - anchorY, z = SectionPos.z(key) - anchorZ;
            int slot = hash(x, y, z) & (buckets - 1);
            while (data.get(slot * 8 + 4) != 0.0F) slot = (slot + 1) & (buckets - 1);
            int start = offset;
            long range = entry.getLongValue();
            int first = (int) (range >>> 32), count = (int) range;
            for (int i = first; i < first + count; i++) {
                LightSource source = visible.get(i);
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
        visible.clear();
        size = 0;
        buckets = 1;
    }

    public static int hash(int x, int y, int z) {
        return x * 73856093 ^ y * 19349663 ^ z * 83492791;
    }
}
