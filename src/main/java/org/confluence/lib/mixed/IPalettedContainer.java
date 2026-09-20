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
}
