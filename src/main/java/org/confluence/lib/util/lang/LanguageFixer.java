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
        if (languageManager != null) {
            String selected = languageManager.getSelected();
            LanguageInfo language = languageManager.getLanguage(selected);
            if (language != null && !"en_us".equals(selected)) {
                modTable.putAll(I18nManager.loadTranslations(selected));
                InputStream stream = NeoForge.class.getResourceAsStream("/assets/neoforge/lang/" + selected + ".json");
                if (stream != null) {
                    try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                        modTable.putAll(new Gson().fromJson(reader, new TypeToken<>() {}));
                    } catch (IOException ignored) {}
                }
                String s = modTable.get("fml.loadingerrorscreen.errorheader");
                if (s != null) {
                    Map<String, String> author = Map.of(
                            "zh_cn", "该界面的本地化功能由汇流来世修复"
                    );
                    modTable.put("fml.loadingerrorscreen.errorheader", s + "\n" + author.getOrDefault(selected, "The i18n of this screen is fixed by Confluence Otherworld"));
                }
            }
        }
    }
}
