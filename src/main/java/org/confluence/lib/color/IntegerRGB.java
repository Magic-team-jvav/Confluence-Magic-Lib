package org.confluence.lib.color;

import java.util.Objects;

public record IntegerRGB(int red, int green, int blue) {
    public static final IntegerRGB VOID_A = of(0x2c182a);
    public static final IntegerRGB VOID_B = of(0x3b2e6b);
    public static final IntegerRGB VOID_C = of(0x3c6f98);
    public static final IntegerRGB VOID_WEAVE_A = of(0x8641f8);
    public static final IntegerRGB VOID_WEAVE_B = of(0x6516e9);
    public static final IntegerRGB VOID_WEAVE_C = of(0x4d57fb);
    public static final IntegerRGB DREAM_BUBBLE_A = of(0xff3a6f);
    public static final IntegerRGB DREAM_BUBBLE_B = of(0xffd03a);
    public static final IntegerRGB DREAM_BUBBLE_C = of(0xb7ff3a);
    public static final IntegerRGB BLACK = of(0x000000);
    public static final IntegerRGB GRAY = of(0x828282);
    public static final IntegerRGB WHITE = of(0xFFFFFF);
    public static final IntegerRGB BLUE = of(0x9696FF);
    public static final IntegerRGB GREEN = of(0x96FF96);
    public static final IntegerRGB ORANGE = of(0xFFC896);
    public static final IntegerRGB LIGHT_RED = of(0xFF9696);
    public static final IntegerRGB PINK = of(0xFF96FF);
    public static final IntegerRGB LIGHT_PURPLE = of(0xD2A0FF);
    public static final IntegerRGB LIME = of(0x96FF0A);
    public static final IntegerRGB YELLOW = of(0xFFFF0A);
    public static final IntegerRGB CYAN = of(0x05C8FF);
    public static final IntegerRGB RED = of(0xFF2864);
    public static final IntegerRGB PURPLE = of(0xB428FF);

    public static IntegerRGB of(int rgb) {
        return new IntegerRGB((rgb & 0xFF0000) >> 16, (rgb & 0x00FF00) >> 8, rgb & 0x0000FF);
    }

    public static IntegerRGB of(int red, int green, int blue) {
        return new IntegerRGB(red, green, blue);
    }

    public IntegerRGB mixture(IntegerRGB another, float anotherRatio) {
        int r = Math.round(red - (red - another.red) * anotherRatio);
        int g = Math.round(green - (green - another.green) * anotherRatio);
        int b = Math.round(blue - (blue - another.blue) * anotherRatio);
        return new IntegerRGB(r, g, b);
    }

    public int get() {
        return (red << 16) + (green << 8) + blue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        return o instanceof IntegerRGB(
                int red1, int green1, int blue1
        ) && red == red1 && blue == blue1 && green == green1;
    }

    @Override
    public int hashCode() {
        return Objects.hash(red, green, blue);
    }
}
