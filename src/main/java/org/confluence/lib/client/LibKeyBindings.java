package org.confluence.lib.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

/// 重力反转的按键（WP6c：整条特性从 1.20 的 Lib 搬到 1.21 的 Lib）。
///
/// 1.20 原文（`Confluence-Magic-Lib` 的 `lib/client/LibKeyBindings.java`）逐字，只有平台/入口两处改写：
/// 1. `PortEventHandler.addListener(LibKeyBindings::registerKeyMapping)`（PortLib 的总线助手，
///    由 `LibKeyBindings.init()` 调用）→ **1.21 落在 `LibClientModEvents#registerKeyMappings`**：
///    那里是注解式订阅（`@EventBusSubscriber(modid = LIB_ID, value = Dist.CLIENT)`），
///    与本类同包同模块的另外三个注册处理器（`RegisterParticleProvidersEvent` 等）一致。
///    ⚠️ **所以本类没有 `init(...)`** —— 1.20 那个无参入口是 PortLib 时代「自己去找模组总线」的钩子，
///    1.21 由注解式订阅替代。把它留在这里会是**死代码**（实测：无调用点）。
///    ⚠️ 也不要把注册挪到 `NeoForge.EVENT_BUS`：`RegisterKeyMappingsEvent` 是 `IModBusEvent`，
///    写错总线**能编译但静默不注册**。
/// 2. `Lazy` 换成 NeoForge 原生的 `net.neoforged.neoforge.common.util.Lazy`（1.20 用的是 Forge 同名类，API 相同）。
///
/// ⚠️ 与任务书描述不同、以仓库实测为准：NeoForge 21.1.219 **仍然有** `KeyConflictContext`
/// （`net.neoforged.neoforge.client.settings.KeyConflictContext`），主模组 `ModKeyBindings:41` 就在用
/// 五参构造器 `new KeyMapping(name, KeyConflictContext.IN_GAME, InputConstants.Type.KEYSYM, key, category)`，
/// 所以本类**保留** 1.20 的形态，不做「原生化」改写。
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
