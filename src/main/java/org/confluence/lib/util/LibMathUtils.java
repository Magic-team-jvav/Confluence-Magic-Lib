package org.confluence.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector2f;

import static java.lang.Math.*;

public final class LibMathUtils {
    public static final float HALF_SQRT_3 = (float) (Math.sqrt(3) / 2.0);
    public static final float INV_255 = 1.0F / 255.0F;

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

    /// 整数乘正数小数得到新整数
    public static int multiplyInt(int original, float factor, RandomSource random) {
        if (factor <= 0) return 0;
        factor = Math.abs(factor);
        int i = (int) factor;
        original *= i;
        if (checkChance(factor - i, random)) {
            ++original;
        }
        return original * Mth.sign(factor);
    }

    /// 整数除正数小数得到新整数
    public static int divideInt(int original, float factor, RandomSource random) {
        if (factor <= 0) return 0;
        factor = Math.abs(factor);
        float f = original / factor;
        original = (int) f;
        if (checkChance(f - original, random)) {
            ++original;
        }
        return original * Mth.sign(factor);
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

    public static double length(double[] arr) {
        if (arr.length == 1) return arr[0];
        if (arr.length == 2) return Mth.length(arr[0], arr[1]);
        if (arr.length == 3) return Mth.length(arr[0], arr[1], arr[2]);
        throw new IllegalArgumentException("Unsupported array length: " + arr.length);
    }

    public static double invertSquare(double value) {
        return 1 / (value * value);
    }

    public static float length(float[] arr) {
        if (arr.length == 1) return arr[0];
        if (arr.length == 2) return (float) Mth.length(arr[0], arr[1]);
        if (arr.length == 3) return (float) Mth.length(arr[0], arr[1], arr[2]);
        throw new IllegalArgumentException("Unsupported array length: " + arr.length);
    }

    public static float invertSquare(float value) {
        return 1 / (value * value);
    }

    /**
     * 计算从点A到点B的角度（弧度），范围 [0, 2π)
     *
     * @param a 起点
     * @param b 终点
     * @return 弧度值，范围 [0, 2π)
     */
    public static float getAngleRadians(Vec2 a, Vec2 b) {
        return getAngleRadians(a.x, a.y, b.x, b.y);
    }

    /**
     * 计算从点(ax, ay)到点(bx, by)的角度（弧度），范围 [0, 2π)
     *
     * @param ax 起点x坐标
     * @param ay 起点y坐标
     * @param bx 终点x坐标
     * @param by 终点y坐标
     * @return 弧度值，范围 [0, 2π)
     */
    public static float getAngleRadians(double ax, double ay, double bx, double by) {
        return (float) (Math.atan2(by - ay, bx - ax)) + (float) Math.PI;
    }

    /**
     * 根据角度和半径计算点的坐标
     *
     * @param radius  半径
     * @param radians 角度（弧度）
     * @return Vec2 坐标点
     */
    public static Vec2 pointFromAngle(float radius, float radians) {
        float x = (float) (radius * Math.cos(radians));
        float y = (float) (radius * Math.sin(radians));
        return new Vec2(x, y);
    }

    /**
     * 判断点是否在圆内
     *
     * @param point  待检测的点
     * @param center 圆心
     * @param radius 半径
     * @return 如果点在圆内返回true
     */
    public static boolean isPointInCircle(Vec2 point, Vec2 center, float radius) {
        return point.distanceToSqr(center) < radius * radius;
    }

    /**
     * Calc a vector2 that equals to a vector2 rotated an angle
     *
     * @param v   origin vector, wont be changed
     * @param deg angle rotated, in degrees
     * @return rotated vector2
     */
    public static Vector2f rotationDegrees(Vector2f v, float deg) {
        return rotate(v, (float) toRadians(deg));
    }

    /**
     * Calc a vector2 that equals to a vector2 rotated an angle
     *
     * @param v origin vector, wont be changed
     * @param d angle rotated, in radians
     * @return rotated vector2
     */
    public static Vector2f rotate(Vector2f v, float d) {
        return new Vector2f(
                (float) (v.x * cos(d) - v.y * sin(d)),
                (float) (v.x * sin(d) + v.y * cos(d))
        );
    }

    public static Vector2f copy(Vector2f v) {
        return new Vector2f(v.x, v.y);
    }

    public static float angle(Vector2f from, Vector2f to) {
        return (float) ((atan2(to.y, to.x) - atan2(from.y, from.x)) % (Math.PI * 2));
    }

    public static float angleDegrees(Vector2f from, Vector2f to) {
        return (float) toDegrees(angle(from, to));
    }

    public static boolean isInRange(double value, double min, double max) {
        if (min > max) {
            double min1 = min;
            min = max;
            max = min1;
        }

        return value > min && value < max;
    }

    public static boolean isInRange(double valueX, double valueY, double minX, double minY, double maxX, double maxY) {
        if (minX > maxX) {
            double minX1 = minX;
            minX = maxX;
            maxX = minX1;
        }
        if (minY > maxY) {
            double minY1 = minY;
            minY = maxY;
            maxY = minY1;
        }

        return valueX > minX && valueX < maxX && valueY > minY && valueY < maxY;
    }

    public static Vec3i dist(BlockPos a, BlockPos b) {
        return new Vec3i(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    public static float safeDiv(float a, float b) {
        if (b == 0F) return 0F;
        return a / b;
    }

    public static double safeDiv(double a, double b) {
        if (b == 0.0) return 0.0;
        return a / b;
    }

    public static float clampWithProportion(float value, float min, float max) {
        if (min > max) {
            float cache = max;
            max = min;
            min = cache;
        }
        float length = max - min;
        if (length == 0)
            throw new IllegalArgumentException("The min value " + min + " cannot be equal to the max value" + max + "!");

        if (value > max) {
            while (value > max + length) {
                value -= length;
            }
            return max - (max - value);
        } else if (value < min) {
            while (value < min + length) {
                value += length;
            }
            return min + (value - min);
        }
        return value;
    }
}
