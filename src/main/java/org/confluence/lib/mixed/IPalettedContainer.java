package org.confluence.lib.mixed;

import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;

/// `PalettedContainer` 的扩展：用它自己的 registry / strategy 造一个只含单个值的新容器。
///
/// 由 `org.confluence.lib.mixin.chunk.PalettedContainerMixin` 实现。
public interface IPalettedContainer<T> {
    /// 创建一个新的、只包含一个元素的 PalettedContainer
    PalettedContainer<T> confluence$recreateSingle(T ele);

    @SuppressWarnings("unchecked")
    static <T> PalettedContainer<T> recreateSingle(PalettedContainerRO<T> container, T t) {
        return ((IPalettedContainer<T>) container).confluence$recreateSingle(t);
    }

    static <T> PalettedContainer<T> copyBiomes(PalettedContainerRO<T> source) {
        PalettedContainer<T> result = source.recreate();
        for (int y = 0; y < 4; y++)
            for (int z = 0; z < 4; z++)
                for (int x = 0; x < 4; x++)
                    result.set(x, y, z, source.get(x, y, z));
        return result;
    }
}
