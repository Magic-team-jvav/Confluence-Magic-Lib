package org.confluence.lib.integration.animation;

import net.neoforged.fml.loading.LoadingModList;

public class AnimationConstants {
    public static final boolean SHOULD_APPLY = LoadingModList.get().getModFileById("geckolib") != null;
}
