package org.confluence.lib.util;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.DataFixer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

public final class LibClientUtils {
    public static NativeImage copyWithGray(NativeImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        NativeImage image = new NativeImage(original.format(), width, height, false);
        image.copyFrom(original);
        int[] pixels = original.getPixelsABGR();
        int[] average = new int[pixels.length];
        int u = 0;
        int d = 255;
        for (int i = 0; i < height; i++) {
            int i1 = i * width;
            for (int j = 0; j < width; j++) {
                int index = j + i1;
                int color = pixels[index];
                int a = color >>> 24;
                int b = color & 255;
                int g = color >> 8 & 255;
                int r = color >> 16 & 255;
                int avg = (int) (r * 0.3F + g * 0.59F + b * 0.11F);
                if (avg > u) u = avg;
                if (avg < d) d = avg;
                average[index] = a << 8 | avg;
            }
        }
        int i1 = u - d;
        float x;
        int y;
        if (94 < i1) { // 94.72F < i1
            x = 94.72F / i1;
            y = 105;
        } else {
            x = 1.0F;
            y = 199 - i1; // 94.72F - i1 + 105
        }
        for (int i = 0; i < height; i++) {
            int i2 = i * width;
            for (int j = 0; j < width; j++) {
                int color = average[j + i2];
                int avg = color & 255;
                avg = (int) ((avg - d) * x) + y;
                int a = color >> 8 & 255;
                image.setPixelABGR(j, i, a << 24 | avg << 16 | avg << 8 | avg);
            }
        }
        return image;
    }

    public static NativeImage copyWithNegative(NativeImage original) {
        return original.mappedCopy(color -> {
            int a = color >>> 24;
            int b = color & 255;
            int g = color >> 8 & 255;
            int r = color >> 16 & 255;
            return ARGB.color(a, 255 - r, 255 - g, 255 - b);
        });
    }

    public static NativeImage replaceWithBlueWhite(int width, int height) {
        NativeImage blueWhite = new NativeImage(width, height, false);
        for (int i = 0; i < height; i++) {
            int color = i % 4 < 2 ? -256 : -1;
            for (int j = 0; j < width; j++) {
                blueWhite.setPixelABGR(j, i, color);
            }
        }
        return blueWhite;
    }

    public static @Nullable Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static GameProfile getGameProfile() {
        return Minecraft.getInstance().getGameProfile();
    }

    public static DataFixer getDataFixer() {
        return Minecraft.getInstance().getFixerUpper();
    }

    public static MutableComponent keyMappingComponent(KeyMapping keyMapping) {
        return MutableComponent.create(keyMapping.getTranslatedKeyMessage().getContents()).withStyle(ChatFormatting.GRAY);
    }
}
