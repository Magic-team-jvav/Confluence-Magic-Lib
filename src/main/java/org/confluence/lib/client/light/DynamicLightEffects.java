package org.confluence.lib.client.light;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.confluence.lib.client.DynamicLightDispatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public final class DynamicLightEffects {
    private static final DynamicLightEffects INSTANCE = new DynamicLightEffects();

    private DynamicLightEffects() {}

    public static void renderBeam(Vec3 start, Vec3 end, int brightness) {
        double distance = start.distanceTo(end);
        if (brightness <= 0 || !Double.isFinite(distance)) return;
        long segments = Math.max(1L, (long) Math.ceil(distance / 4.0));
        int strength = Mth.clamp(brightness, 1, 15) * 17;
        for (long index = 0; index < segments; index++) {
            DynamicLightDispatcher.INSTANCE.addLightSource(start.lerp(end, index / (double) segments), strength);
        }
        DynamicLightDispatcher.INSTANCE.addLightSource(end, strength);
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

    private final List<Pulse> sources = new ArrayList<>();
    private ClientLevel currentLevel;

    private void add(ClientLevel level, Vec3 position, int brightness, int duration) {
        useLevel(level);
        if (level == null || brightness <= 0 || duration <= 0
                || !Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z))
            return;
        long now = level.getGameTime();
        sources.add(new Pulse(position, Mth.clamp(brightness, 1, 15), now, now + (long) duration));
    }

    private void render(ClientLevel level, float partialTick, BiConsumer<Vec3, Integer> consumer) {
        useLevel(level);
        if (level == null) return;
        long now = level.getGameTime();
        // 批量移除结束的闪光，避免 ArrayList 逐个删除产生反复搬移。
        sources.removeIf(pulse -> now < pulse.started() || now >= pulse.expires());
        for (Pulse pulse : sources) {
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
