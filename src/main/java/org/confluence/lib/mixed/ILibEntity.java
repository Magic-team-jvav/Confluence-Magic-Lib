package org.confluence.lib.mixed;

import net.minecraft.world.entity.Entity;

public interface ILibEntity extends SelfGetter<Entity> {
    void confluence$setShouldRot(boolean bool);

    boolean confluence$isShouldRot();

    float confluence$getDimensionHeight();

    static ILibEntity of(Entity entity) {
        return (ILibEntity) entity;
    }
}
