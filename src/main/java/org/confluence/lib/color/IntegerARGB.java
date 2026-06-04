package org.confluence.lib.color;

import java.util.Objects;

public record IntegerARGB(int alpha, int red, int green, int blue) {
    public static IntegerARGB of(int argb) {
        return new IntegerARGB((argb & 0xFF000000) >> 24, (argb & 0x00FF0000) >> 16, (argb & 0x0000FF00) >> 8, argb & 0x000000FF);
    }

    public IntegerARGB mixture(IntegerARGB another, float anotherRatio) {
        int r = Math.round(red - (red - another.red) * anotherRatio);
        int g = Math.round(green - (green - another.green) * anotherRatio);
        int b = Math.round(blue - (blue - another.blue) * anotherRatio);
        int a = Math.round(alpha - (alpha - another.alpha) * anotherRatio);
        return new IntegerARGB(r, g, b, a);
    }

    public int get() {
        return (alpha << 24) + (red << 16) + (green << 8) + blue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof IntegerARGB i && alpha == i.alpha() && red == i.red() && green == i.green() && blue == i.blue();
    }

    @Override
    public int hashCode() {
        return Objects.hash(alpha, red, green, blue);
    }
}
