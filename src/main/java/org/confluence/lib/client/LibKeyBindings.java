package org.confluence.lib.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;
import org.mesdag.portlib.event.PortEventHandler;

public final class LibKeyBindings {
    public static final Lazy<KeyMapping> FLIP_GRAVITATION = Lazy.of(() -> new KeyMapping(
            "key.confluence_magic_lib.flip_gravitation",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UP,
            category()
    ));

    public static void init() {
        PortEventHandler.addListener(LibKeyBindings::registerKeyMapping);
    }

    private static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(FLIP_GRAVITATION.get());
    }

    private static String category() {
        return "key.confluence_magic_lib.gameplay"; // confluence mixin here
    }
}
