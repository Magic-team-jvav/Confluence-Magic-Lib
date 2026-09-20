package org.confluence.lib.api.animation.third_person;

import net.minecraftforge.fml.loading.LoadingModList;

public class AnimationConstants {
    public static final boolean SHOULD_APPLY = LoadingModList.get().getModFileById("geckolib") != null;
    public static final boolean WITH_PARTICLE = LoadingModList.get().getModFileById("particlestorm") != null;
}
