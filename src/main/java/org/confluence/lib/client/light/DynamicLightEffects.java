package org.confluence.lib.client.light;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.DynamicLightDispatcher;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class DynamicLightEffects {
    private static final DynamicLightEffects INSTANCE = new DynamicLightEffects();

    private DynamicLightEffects() {}

    public static void renderBeam(Vec3 start, Vec3 end, int brightness) {
        double distance = start.distanceTo(end);
        if (brightness <= 0 || !Double.isFinite(distance)) return;
        int segments = Mth.clamp((int) Math.ceil(distance / 4.0), 1, 15);
        int strength = Mth.clamp(brightness, 1, 15) * 17;
        for (int index = 0; index <= segments; index++) {
            DynamicLightDispatcher.INSTANCE.addLightSource(start.lerp(end, index / (double) segments), strength);
        }
    }

    public static void flash(Vec3 position, int brightness, int duration) {
        INSTANCE.add(Minecraft.getInstance().level, position, brightness, duration);
    }

    public static void collect(float partialTick) {
        INSTANCE.render(Minecraft.getInstance().level, partialTick,
                (position, brightness) -> DynamicLightDispatcher.INSTANCE.addLightSource(position, brightness * 17));
    }

    public static void clearWorld() {
        INSTANCE.reset();
    }

    private static final int MAX_SOURCES = 64;
    private final Map<BlockPos, Pulse> sources = new LinkedHashMap<>();
    private ClientLevel currentLevel;

    private void add(ClientLevel level, Vec3 position, int brightness, int duration) {
        useLevel(level);
        if (level == null || brightness <= 0 || duration <= 0
                || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z))
            return;
        long now = level.getGameTime();
        sources.values().removeIf(pulse -> now < pulse.started() || now >= pulse.expires());
        BlockPos key = BlockPos.containing(position);
        Pulse previous = sources.get(key);
        int strength = Mth.clamp(brightness, 1, 15);
        long expires = now + Math.min(duration, 20);
        // 同一格的特效聚合成一个光源，避免散弹和密集粒子成倍增加开销。
        if (previous != null) {
            strength = Math.max(strength, previous.brightnessAt(now, 0.0F));
            expires = Math.max(expires, previous.expires());
        } else if (sources.size() >= MAX_SOURCES) {
            sources.remove(sources.keySet().iterator().next());
        }
        sources.put(key, new Pulse(position, strength, now, expires));
    }

    private void render(ClientLevel level, float partialTick, BiConsumer<Vec3, Integer> consumer) {
        useLevel(level);
        if (level == null) return;
        long now = level.getGameTime();
        var iterator = sources.values().iterator();
        while (iterator.hasNext()) {
            Pulse pulse = iterator.next();
            if (now < pulse.started() || now >= pulse.expires()) {
                iterator.remove();
                continue;
            }
            int brightness = pulse.brightnessAt(now, partialTick);
            if (brightness > 0) consumer.accept(pulse.position(), brightness);
        }
    }

    private void reset() {
        sources.clear();
        currentLevel = null;
    }

    private void useLevel(ClientLevel level) {
        if (currentLevel != level) {
            sources.clear();
            currentLevel = level;
        }
    }

    private record Pulse(Vec3 position, int brightness, long started, long expires) {
        int brightnessAt(long now, float partialTick) {
            float remaining = (expires - now - Mth.clamp(partialTick, 0.0F, 1.0F)) / (expires - started);
            return Math.round(brightness * remaining);
        }
    }
}
