package org.confluence.lib.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

public final class LibKeyBindings {
    public static final Lazy<KeyMapping> FLIP_GRAVITATION = Lazy.of(() -> new KeyMapping(
            "key.confluence_magic_lib.flip_gravitation",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            category()
    ));

    private static String category() {
        return "key.confluence_magic_lib.gameplay";
    }
}
