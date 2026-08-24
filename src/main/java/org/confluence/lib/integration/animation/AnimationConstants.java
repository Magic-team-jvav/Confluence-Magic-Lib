package org.confluence.lib.integration.animation;

import net.minecraftforge.fml.loading.LoadingModList;

public class AnimationConstants {
    public static final boolean SHOULD_APPLY = LoadingModList.get().getModFileById("geckolib") != null;
}
