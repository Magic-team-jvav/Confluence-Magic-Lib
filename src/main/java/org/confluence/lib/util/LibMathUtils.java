package org.confluence.lib.util;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

public final class LibMathUtils {
    /// @author ChatGPT
    public static float cubicBezier(float t, float p0, float p1, float p2, float p3) {
        float u = 1 - t;
        float tt = t * t;
        float uu = u * u;
        float uuu = uu * u;
        float ttt = tt * t;
        return uuu * p0 + 3 * uu * t * p1 + 3 * u * tt * p2 + ttt * p3;
    }

    public static boolean checkChance(float value, RandomSource random) {
        return value >= 1.0F || (value > 0.0F && random.nextFloat() < value);
    }

    public static boolean checkChance(double value, RandomSource random) {
        return value >= 1.0 || (value > 0.0 && random.nextDouble() < value);
    }

    /// 整数乘非负小数得到新整数
    public static int multiplyInt(int original, float factor, RandomSource random) {
        int sign = Mth.sign(factor);
        if (sign == 0) {
            return 0;
        }
        factor = Math.abs(factor);
        int i = (int) factor;
        original *= i;
        if (checkChance(factor - i, random)) {
            ++original;
        }
        return original * sign;
    }

    /// 整数除正数小数得到新整数
    public static int divideInt(int original, float factor, RandomSource random) {
        int sign = Mth.sign(factor);
        if (sign == 0) {
            return 0;
        }
        factor = Math.abs(factor);
        float f = original / factor;
        original = (int) f;
        if (checkChance(f - original, random)) {
            ++original;
        }
        return original * sign;
    }

    /// o-t.....o-f____o____o+f.....o+t
    ///
    /// @param original middle point
    /// @param from     positive integer
    /// @param to       positive integer
    /// @return value belongs to \[o-t, o-f\] or \[o+f, o+t\]
    public static double randomFromTo(RandomSource random, double original, double from, double to) {
        if (from >= to) {
            throw new IllegalArgumentException("from must be less than to, currently is " + from + " >= " + to);
        }
        if (from <= 0) {
            throw new IllegalArgumentException("from must be positive, currently is " + from);
        }
        if (random.nextBoolean()) {
            return Mth.nextDouble(random, original + from, original + to);
        }
        return Mth.nextDouble(random, original - to, original - from);
    }

    public static int randomFromTo(RandomSource random, int original, int from, int to) {
        if (from >= to) {
            throw new IllegalArgumentException("from must be less than to, currently is " + from + " >= " + to);
        }
        if (from <= 0) {
            throw new IllegalArgumentException("from must be positive, currently is " + from);
        }
        if (random.nextBoolean()) {
            return Mth.nextInt(random, original + from, original + to);
        }
        return Mth.nextInt(random, original - to, original - from);
    }
}
