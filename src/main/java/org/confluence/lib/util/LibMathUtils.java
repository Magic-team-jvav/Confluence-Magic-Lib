package org.confluence.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;

import java.util.function.ToDoubleFunction;

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

    /// 计算暴击伤害，如果触发暴击则伤害×1.5。
    ///
    /// 1.20 侧同名方法在 `Confluence-Magic-Lib` 的 `LibMathUtils.java`（紧接着 `checkChance` 之后），
    /// 逐字搬运。1.21 侧此前缺这个成员（又一个「成员级盲区」）。
    ///
    /// 消费点：**枪械内联 G4′** 的服务端开火管线 `GunFiringService.java:39`
    /// （1.20 `common/combat/gun/GunFiringService.java:37` 同一行）。
    public static float criticalDamageTotal(float critical, float damage, RandomSource random) {
        return checkChance(critical, random) ? damage * 1.5F : damage;
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

    /// 从 A 指向 B 的单位向量（1.20 `LibMathUtils:502` 同名方法；1.21 侧此前缺这一支，
    /// 由枪械 G3′ 的 `BaseBulletEntity` 回补，见 `notes/WP4-BATCH25-WIP.md`）。
    public static Vec3 getVectorA2B(Entity a, Entity b) {
        return b.position().subtract(a.position()).normalize();
    }

    // ------------------------------------------------------------------
    // 追踪弹道基：`interpolateBasis` 家族（1.20 `LibMathUtils:366-479` 逐字搬入）
    // ------------------------------------------------------------------
    //
    // 1.21 侧此前**这四个方法都没有**；消费方是主模组的 `TheDestroyer`（1.20 `:228` 用它算追踪
    // 转向/加速），随「WP3 客户端族批」的服务端半一起补。四个方法互相依赖：
    // `interpolateBasis` → `vectorProjection`，`getLerp`/`getThresholdInterpolator` 是喂给它的插值器工厂。

    /// 将currDir方向的单位向量记为v1, 我们使用向量投影的方式构造单位向量v2，使得v1与v2为currDir, targetDir平面上的基，且v1垂直于v2.
    ///
    /// 将currDir的长度写为c，则currDir=cv1+0v2；另，有a,b使得targetDir可被写成av1+bv2.
    ///
    /// 注意到，\[c,0\]转换到\[a,b\]需要一个旋转+缩放；同样的，对于基{v1,v2}中，这一系数的变换也将currDir变换到targetDir.
    ///
    /// 然而，一般而言，我们希望追踪弹幕每次只进行这一变换的一部分以获得更合理的弹道。
    ///
    /// 因此，我们对旋转角和缩放长度进行插值。至此，我们即可获得最终的方向向量。
    ///
    /// 注：若我们把v1看做x轴，v2看做y轴，则角度插值和角度均应在一二象限。
    ///
    /// 另，从以上过程中可知，**targetDir的方向和长度都至关重要**。
    ///
    /// @param currDir            当前弹幕的方向向量
    /// @param targetDir          方向向量，记录了追踪的最终方向与长度（想要达到的速度）
    /// @param angleInterpolator  提供角度插值；输入为当前方向和追踪方向的角度差，输出为追踪所变换的角度
    /// @param lengthInterpolator 提供向量长度（即速度）插值；输入为当前方向和追踪方向的长度差，输出为追踪所变换的向量长度
    /// @return 变换完毕的向量
    public static Vec3 interpolateBasis(
            Vec3 currDir,
            Vec3 targetDir,
            ToDoubleFunction<Double> angleInterpolator,
            ToDoubleFunction<Double> lengthInterpolator
    ) {
        double currDirLen = currDir.length();
        double targetDirLen = targetDir.length();
        // 以下多次用到仅单次使用的乘数的设计，干脆公用同一个变量
        double multi;
        // 起始向量与目标向量均为0，直接返回原向量
        if (currDirLen < 1e-5 && targetDirLen < 1e-5) {
            return currDir;
        }
        // 若仅有起始速度为0，则直接返回0向量到目标向量的插值
        if (currDir.lengthSqr() < 1e-9) {
            multi = lengthInterpolator.applyAsDouble(targetDirLen) / targetDirLen;
            return targetDir.multiply(multi, multi, multi);
        }
        // 若仅有终止速度为0，则直接返回起始向量到0向量的线性插值，即起始向量*(1-进度)。
        if (targetDir.lengthSqr() < 1e-9) {
            multi = 1 - (lengthInterpolator.applyAsDouble(currDirLen) / currDirLen);
            return currDir.multiply(multi, multi, multi);
        }
        // 此时，起始终止速度均不为0，后续操作不会造成NaN值。按照注释中的步骤获得结果。
        multi = 1 / currDirLen;
        Vec3 v1 = currDir.multiply(multi, multi, multi);
        Vec3 v1Component = vectorProjection(targetDir, v1);
        Vec3 v2 = targetDir.subtract(v1Component);
        double a, b; // 此处的a,b见上方的方法注释中说明
        double v1CompLen = v1Component.length();
        double v2Len = v2.length();
        // 夹角大于pi/2时，即cos(theta)<0或v1·v1Component<0时，a是负数
        a = v1CompLen * Math.signum(v1.dot(v1Component));
        // 此处的v2方向正确，但尚未转化为单位向量；若v2近似地为0, 即v1与v2共线。
        if (v2Len < 1e-5) {
            b = 0;
        } else {
            b = v2Len;
            multi = 1 / v2Len;
            v2 = v2.multiply(multi, multi, multi);
        }
        // targetDir = [a,b]·[v1,v2]; angleRad = angle([1,0], [a,b]) = atan2(b,a)
        double angleRad = Math.atan2(b, a);
        // 计算角度插值
        double angleDelta = angleInterpolator.applyAsDouble(angleRad);
        // 获得旋转后的方向；此时方向向量为单位向量。
        multi = Math.cos(angleDelta);
        Vec3 result = v1.multiply(multi, multi, multi);
        multi = Math.sin(angleDelta);
        result = result.add(v2.multiply(multi, multi, multi));
        // 计算长度插值
        double length = currDirLen + lengthInterpolator.applyAsDouble(targetDirLen - currDirLen);
        return result.multiply(length, length, length);
    }

    /// 返回一个可以被interpolateBasis作为angleInterpolator或lengthInterpolator使用的线性插值。
    ///
    /// 即，若progress为0，则插值一定提供0，在追踪中表现为不追踪；
    ///
    /// 若progress为1，则插值一定提供全额变化值，在追踪中表现为瞬间完全调整方向。
    ///
    /// 例：progress为0.5，则插值一定提供变化值的一半，在追踪中表现为方向（弧度）/速度 *误差越大，调整速度越快*。
    ///
    /// @param progress 插值强度；越接近0越弱，越接近1越强。取值范围 - [0, 1]
    /// @return 插值ToDoubleFunction
    public static ToDoubleFunction<Double> getLerp(double progress) {
        return x -> x * progress;
    }

    /// 返回一个可以被interpolateBasis作为angleInterpolator或lengthInterpolator使用的阈值式插值。
    ///
    /// 即，若progress为0，则插值一定提供0，在追踪中表现为不追踪；
    ///
    /// 否则，插值提供 变化值 与 阈值 中更小的一者，在追踪中表现为方向（弧度）/速度的误差以 *恒定的效率* 被修正。
    ///
    /// **再次注意：方向（弧度）的插值单位为弧度而非角度！**
    ///
    /// @param efficiency 插值强度；越接近0越弱，越高越强。取值范围 - [0, inf)
    /// @return 插值ToDoubleFunction
    public static ToDoubleFunction<Double> getThresholdInterpolator(double efficiency) {
        return x -> Math.min(x, efficiency);
    }

    /// 向量投影；**toProjectOnto不可以为0向量**！
    ///
    /// @param vector        被投影的向量
    /// @param toProjectOnto 投影的目标向量
    /// @return 投影结果
    public static Vec3 vectorProjection(Vec3 vector, Vec3 toProjectOnto) {
        double sqr = toProjectOnto.lengthSqr();
        if (sqr == 0.0)
            throw new IllegalArgumentException("Length of toProjectOnto could not be zero");
        return toProjectOnto.scale(toProjectOnto.dot(vector) / sqr);
    }
}
