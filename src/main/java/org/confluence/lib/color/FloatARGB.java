package org.confluence.lib.color;

import net.minecraft.util.Mth;

import java.util.Objects;

public record FloatARGB(float alpha, float red, float green, float blue) {
    public FloatARGB mixture(FloatARGB another, float anotherRatio) {
        float a = Mth.clamp(alpha - (alpha - another.alpha) * anotherRatio, 0.0F, 1.0F);
        float r = Mth.clamp(red - (red - another.red) * anotherRatio, 0.0F, 1.0F);
        float g = Mth.clamp(green - (green - another.green) * anotherRatio, 0.0F, 1.0F);
        float b = Mth.clamp(blue - (blue - another.blue) * anotherRatio, 0.0F, 1.0F);
        return new FloatARGB(a, r, g, b);
    }

    public int get() {
        return ((int) (alpha * 255) << 24) + ((int) (red * 255) << 16) + ((int) (green * 255) << 8) + (int) (blue * 255);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof FloatARGB(
                float a, float r, float g, float b
        ) && alpha == a && red == r && green == g && blue == b;
    }

    @Override
    public int hashCode() {
        return Objects.hash(alpha, red, green, blue);
    }
}
