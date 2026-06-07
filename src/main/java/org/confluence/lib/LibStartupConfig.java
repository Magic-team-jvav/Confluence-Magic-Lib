package org.confluence.lib;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModFileInfo;
import org.apache.commons.codec.digest.Md5Crypt;
import org.confluence.lib.util.LibUtils;
import org.mesdag.portlib.config.PortConfigSpec;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class LibStartupConfig {
    public static volatile PortConfigSpec.ListValue<String> ATTRIBUTE_REPLACE;
    private static volatile PortConfigSpec.LongValue modifyTime;
    private static volatile PortConfigSpec.StringValue version;
    private static volatile PortConfigSpec.StringValue messageDigest;
    private static volatile PortConfigSpec.IntValue alarmTimes;
    private static volatile PortConfigSpec.BooleanValue ITEM_GROUPS;
    private static volatile PortConfigSpec SPEC;

    static synchronized void register() {
        PortConfigSpec.Builder builder = PortConfigSpec.builder(ConfluenceMagicLib.LIB_ID);
        ATTRIBUTE_REPLACE = builder.defineList("attributeReplacements", List.of(
                "crit_chance = confluence_magic_lib:generic.critical_chance",
                "ranged_damage = confluence_magic_lib:generic.ranged_damage",
                "dodge_chance = confluence_magic_lib:generic.dodge_chance",
                "magic_damage = confluence_magic_lib:generic.magic_damage",
                "armor_penetration = confluence_magic_lib:generic.armor_penetration"
        ));
        modifyTime = builder.defineInRange("modifyTime", -1L, Long.MIN_VALUE, Long.MAX_VALUE, null);
        version = builder.define("version", "", null);
        messageDigest = builder.define("messageDigest", "", null);
        alarmTimes = builder.defineInRange("alarmTimes", -1, Integer.MIN_VALUE, Integer.MAX_VALUE, null);
        ITEM_GROUPS = builder.define("itemGroups", true, null);
        SPEC = builder.build();
        SPEC.load();
    }

    private static boolean shouldAlarmInThisJVM = true;

    public static void checkIfSomeoneHasViolatedEULA(Player player) {
        if (!shouldAlarmInThisJVM || LibUtils.isDev() || LibUtils.isPhysicalServer()) return;
        IModFileInfo modFileInfo = ModList.get().getModFileById(ConfluenceMagicLib.CONFLUENCE_ID);
        if (modFileInfo == null) return;
        boolean shouldSave = false;
        Path filePath = modFileInfo.getFile().getFilePath();

        try {
            String currentVersion = modFileInfo.versionString();
            String lastVersion = version.get();
            String currentPath = Md5Crypt.md5Crypt(filePath.toString().getBytes(StandardCharsets.UTF_8), "$1$tur3m1an");
            String lastPath = messageDigest.get();
            long currentModifyTime = Files.getLastModifiedTime(filePath).toMillis();
            long lastModifyTime = modifyTime.getAsLong();
            int lastAlarmTimes = alarmTimes.getAsInt();

            if ((lastVersion.isEmpty() || lastPath.isEmpty() || lastModifyTime == -1 || lastAlarmTimes == -1) || // initialize
                    (!currentVersion.equals(lastVersion) && currentPath.equals(lastPath)) // reset while version changed and path not changes
            ) {
                shouldSave = true;
                modifyTime.set(currentModifyTime);
                version.set(currentVersion);
                messageDigest.set(currentPath);
                alarmTimes.set(3);
            }

            if (lastModifyTime == currentModifyTime && !lastPath.equals(currentPath)) { // if modified time not changes && path changed
                shouldSave = true;
                modifyTime.set(currentModifyTime);
                if (lastAlarmTimes > 0) {
                    alarmTimes.set(lastAlarmTimes - 1);
                    player.sendSystemMessage(Component.translatable("alarm.confluence_magic_lib.this_is_free_mod"));
                }
            }
        } catch (Exception e) {
            ConfluenceMagicLib.LOGGER.debug(e.getMessage());
        } finally {
            if (shouldSave) {
                SPEC.save();
            }
            shouldAlarmInThisJVM = false;
        }
    }

    public static boolean itemGroups() {
        return ITEM_GROUPS != null && ITEM_GROUPS.get();
    }
}
