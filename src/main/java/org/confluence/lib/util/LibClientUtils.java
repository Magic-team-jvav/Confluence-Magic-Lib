package org.confluence.lib.util;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.util.Function4;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ClampedItemPropertyFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.FastColor;
import net.minecraft.world.entity.player.Player;
import org.confluence.lib.common.item.IFunctionCouldEnable;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LibClientUtils {
    public static final ClampedItemPropertyFunction COULD_ENABLE_PROPERTY_FUNCTION = (stack, level, living, seed) -> {
        CompoundTag tag = LibUtils.getItemStackNbtIfPresent(stack);
        return tag != null && tag.getBoolean(IFunctionCouldEnable.DISABLE_KEY) ? 0 : 1;
    };

    /// 将游戏缓存的贴图写入文件
    ///
    /// @param nativeImage 游戏缓存的贴图
    /// @param path        文件全路径，比如<code>FMLPaths.GAMEDIR.get().resolve("redstone.png")</code>
    /// @param argbMixer   argb的混合方法
    public static void writeImageToFile(NativeImage nativeImage, Path path, Function4<Integer, Integer, Integer, Integer, Integer> argbMixer) {
        int[] pixels = nativeImage.getPixelsRGBA();
        Path parent = path.getParent();
        try {
            if (!Files.exists(parent)) Files.createDirectories(parent);
            BufferedImage image = new BufferedImage(nativeImage.getWidth(), nativeImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
            for (int i = 0; i < nativeImage.getHeight(); ++i) {
                for (int j = 0; j < nativeImage.getWidth(); ++j) {
                    int color = pixels[j + i * nativeImage.getWidth()];
                    int a = color >>> 24;
                    int b = color >> 16 & 255;
                    int g = color >> 8 & 255;
                    int r = color & 255;
                    image.setRGB(j, i, argbMixer.apply(a, r, g, b));
                }
            }
            ImageIO.write(image, "png", path.toFile());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write native image to " + path, e);
        }
    }

    public static void writeRawImageToFile(NativeImage nativeImage, Path path) {
        writeImageToFile(nativeImage, path, (a, r, g, b) -> a << 24 | r << 16 | g << 8 | b);
    }

    public static NativeImage copyWithGray(NativeImage original) {
        int width = original.getWidth();
        int height = original.getHeight();
        NativeImage image = new NativeImage(original.format(), width, height, false);
        image.copyFrom(original);
        int[] pixels = original.getPixelsRGBA();
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
                image.setPixelRGBA(j, i, FastColor.ARGB32.color(a, avg, avg, avg));
            }
        }
        return image;
    }

    public static NativeImage copyWithNegative(NativeImage original) {
        NativeImage image = new NativeImage(original.format(), original.getWidth(), original.getHeight(), false);
        image.copyFrom(original);
        image.applyToAllPixels(color -> {
            int a = color >>> 24;
            int b = color & 255;
            int g = color >> 8 & 255;
            int r = color >> 16 & 255;
            return FastColor.ARGB32.color(a, 255 - r, 255 - g, 255 - b);
        });
        return image;
    }

    public static NativeImage replaceWithBlueWhite(int width, int height) {
        NativeImage blueWhite = new NativeImage(width, height, false);
        for (int i = 0; i < height; i++) {
            int color = i % 4 < 2 ? -256 : -1;
            for (int j = 0; j < width; j++) {
                blueWhite.setPixelRGBA(j, i, color);
            }
        }
        return blueWhite;
    }

    public static @Nullable Player getPlayer() {
        return Minecraft.getInstance().player;
    }

    public static GameProfile getGameProfile() {
        return Minecraft.getInstance().getUser().getGameProfile();
    }

    public static DataFixer getDataFixer() {
        return Minecraft.getInstance().getFixerUpper();
    }

    public static MutableComponent keyMappingComponent(KeyMapping keyMapping) {
        return keyMappingComponent(keyMapping, ChatFormatting.GRAY);
    }

    public static MutableComponent keyMappingComponent(KeyMapping keyMapping, ChatFormatting color) {
        return MutableComponent.create(keyMapping.getTranslatedKeyMessage().getContents()).withStyle(color);
    }
}
