package org.confluence.lib.common.entitiy;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public interface IAxisZRotate {
    default void rotateZ(Rotate rotate, Entity entity, float radius) {
        rotateZ(rotate, Mth.lengthSquared(
                entity.getX() - entity.xOld,
                entity.getY() - entity.yOld,
                entity.getZ() - entity.zOld
        ), radius);
    }

    default void rotateZ(Rotate rotate, double velocitySqr, float radius) {
        if (velocitySqr > 0) {
            float r = (float) Math.sqrt(velocitySqr) / radius;
            if (rotate.neo > Mth.TWO_PI) rotate.neo -= Mth.TWO_PI;
            rotate.old = rotate.neo;
            rotate.neo += r / Mth.PI;
        } else {
            rotate.old = rotate.neo;
        }
    }

    class Rotate {
        public float neo = 0;
        public float old = 0;

        public boolean different() {
            return neo != old;
        }

        @Override
        public String toString() {
            return "Rotate{" +
                    "neo=" + neo +
                    ", old=" + old +
                    '}';
        }
    }
}
