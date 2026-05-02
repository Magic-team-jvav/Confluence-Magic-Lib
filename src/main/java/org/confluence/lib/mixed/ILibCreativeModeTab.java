package org.confluence.lib.mixed;

import net.minecraft.world.item.CreativeModeTab;

public interface ILibCreativeModeTab {
    void confluence$buildGroup();

    static ILibCreativeModeTab of(CreativeModeTab tab) {
        return (ILibCreativeModeTab) tab;
    }
}
