package org.confluence.lib.client;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.function.ToIntFunction;

@FunctionalInterface
public interface DynamicLightProvider<T> {
    @Nullable DynamicLightDispatcher.LightSource getLuminance(T instance, float partialTick);

    // 原版亮度等级 0～15；实体中心的位置插值由 provider 统一处理。
    static <T extends Entity> EntityLight<T> entity(int luminance) {
        int brightness = Mth.clamp(luminance, 0, 15);
        return entity(instance -> brightness);
    }

    static <T extends Entity> EntityLight<T> entity(ToIntFunction<T> luminance) {
        return new EntityLight<>(luminance, null);
    }

    /// 带颜色的实体光源；color 返回 0xRRGGBB，只在该 provider 给出最大亮度时才会调用。
    static <T extends Entity> EntityLight<T> entity(ToIntFunction<T> luminance, ToIntFunction<T> color) {
        return new EntityLight<>(luminance, color);
    }

    final class EntityLight<T extends Entity> implements DynamicLightProvider<T> {
        private final ToIntFunction<T> luminance;
        private final @Nullable ToIntFunction<T> color;

        private EntityLight(ToIntFunction<T> luminance, @Nullable ToIntFunction<T> color) {
            this.luminance = luminance;
            this.color = color;
        }

        public int color(T entity) {
            return color == null ? DynamicLightDispatcher.LightSource.WHITE : color.applyAsInt(entity);
        }

        public int brightness(T entity) {
            return Mth.clamp(luminance.applyAsInt(entity), 0, 15);
        }

        @Override
        public @Nullable DynamicLightDispatcher.LightSource getLuminance(T entity, float partialTick) {
            int brightness = brightness(entity);
            return brightness == 0 ? null : source(entity, partialTick, brightness, color(entity));
        }

        static DynamicLightDispatcher.LightSource source(Entity entity, float partialTick, int brightness) {
            return source(entity, partialTick, brightness, DynamicLightDispatcher.LightSource.WHITE);
        }

        static DynamicLightDispatcher.LightSource source(Entity entity, float partialTick, int brightness, int color) {
            Vec3 position = new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()),
                    Mth.lerp(partialTick, entity.yo, entity.getY()) + entity.getBbHeight() * 0.5,
                    Mth.lerp(partialTick, entity.zo, entity.getZ()));
            return new DynamicLightDispatcher.LightSource(position, brightness * 17, color);
        }
    }
}
