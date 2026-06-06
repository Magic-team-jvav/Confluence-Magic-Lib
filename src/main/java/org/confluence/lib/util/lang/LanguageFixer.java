package org.confluence.lib.util.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.resources.language.LanguageManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LanguageFixer {
    private static void confluence$fix(Map<String, String> modTable) {
        if (FMLEnvironment.dist.isDedicatedServer()) return;
        LanguageManager languageManager = Minecraft.getInstance().getLanguageManager();
        if (languageManager == null) return;
        String selected = languageManager.getSelected();
        LanguageInfo language = languageManager.getLanguage(selected);
        if (language == null || "en_us".equals(selected)) return;
        InputStream stream = MinecraftForge.class.getResourceAsStream("/assets/forge/lang/" + selected + ".json");
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
