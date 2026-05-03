package org.confluence.lib.util.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.neoforged.fml.i18n.I18nManager;
import net.neoforged.neoforge.common.NeoForge;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LanguageFixer {
    /// [net.neoforged.neoforge.server.LanguageHook#loadBuiltinLanguages]
    public static void fix(Map<String, String> modTable) {
        LanguageManager languageManager = Minecraft.getInstance().getLanguageManager();
        if (languageManager == null) return;
        String selected = languageManager.getSelected();
        LanguageInfo language = languageManager.getLanguage(selected);
        if (language == null || "en_us".equals(selected)) return;
        InputStream stream = NeoForge.class.getResourceAsStream("/assets/neoforge/lang/zh_cn.json");
        if (stream == null) return;
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            modTable.putAll(new Gson().fromJson(reader, new TypeToken<>() {}));
            modTable.putAll(I18nManager.loadTranslations(selected));
        } catch (IOException ignored) {}
    }
}
