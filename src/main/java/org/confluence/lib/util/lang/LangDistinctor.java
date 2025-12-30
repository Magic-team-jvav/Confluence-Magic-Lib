package org.confluence.lib.util.lang;

import net.minecraft.util.GsonHelper;
import org.confluence.lib.util.LibUtils;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class LangDistinctor {
    public static void main(String[] args) {
        if (!LibUtils.isDev() || args.length == 0) return;
        Path dir = Paths.get(args[0]).resolve("i18n");
        Path en_us = dir.resolve("en_us.json");
        Path zh_cn = dir.resolve("zh_cn.json");
        try (Reader reader0 = new FileReader(zh_cn.toFile()); Reader reader1 = new FileReader(en_us.toFile())) {
            Set<String> allKeys = new HashSet<>(GsonHelper.parse(reader0).keySet());
            allKeys.removeAll(GsonHelper.parse(reader1).keySet());
            for (String key : allKeys) {
                System.out.println(key);
            }
        } catch (Exception ignored) {}
    }
}
