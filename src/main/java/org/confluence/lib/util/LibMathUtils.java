package org.confluence.lib.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.function.ToDoubleFunction;

public final class LibMathUtils {
    public static final float HALF_SQRT_3 = (float) (Math.sqrt(3) / 2.0);
    public static final float INV_255 = 1.0F / 255.0F;

    /**
     * 将HSV转换为ARGB颜色
     */
    public static int hsvToArgb(float hue, float saturation, float value, int alpha) {
        int i = (int) (hue * 6.0F) % 6;
        float f = hue * 6.0F - (float) i;
        float f1 = value * (1.0F - saturation);
        float f2 = value * (1.0F - f * saturation);
        float f3 = value * (1.0F - (1.0F - f) * saturation);
        float f4;
        float f5;
        float f6;
        switch (i) {
            case 0:
                f4 = value;
                f5 = f3;
                f6 = f1;
                break;
            case 1:
                f4 = f2;
                f5 = value;
                f6 = f1;
                break;
            case 2:
                f4 = f1;
                f5 = value;
                f6 = f3;
                break;
            case 3:
                f4 = f1;
                f5 = f2;
                f6 = value;
                break;
            case 4:
                f4 = f3;
                f5 = f1;
                f6 = value;
                break;
            case 5:
                f4 = value;
                f5 = f1;
                f6 = f2;
                break;
            default:
                throw new RuntimeException("Failed to convert HSV to RGB. Input was " + hue + ", " + saturation + ", " + value);
        }

        return FastColor.ARGB32.color(alpha, Mth.clamp((int) (f4 * 255.0F), 0, 255), Mth.clamp((int) (f5 * 255.0F), 0, 255), Mth.clamp((int) (f6 * 255.0F), 0, 255));
    }


    /// 计算三次贝塞尔曲线在指定进度上的插值结果。
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

    /// 计算从点A到点B的角度（弧度），范围 [0, 2π)
    ///
    /// @param a 起点
    /// @param b 终点
    /// @return 弧度值，范围 [0, 2π)
    public static float getAngleRadians(Vec2 a, Vec2 b) {
        return getAngleRadians(a.x, a.y, b.x, b.y);
    }

    /// 计算从点(ax, ay)到点(bx, by)的角度（弧度），范围 [0, 2π)
    ///
    /// @param ax 起点x坐标
    /// @param ay 起点y坐标
    /// @param bx 终点x坐标
    /// @param by 终点y坐标
    /// @return 弧度值，范围 [0, 2π)
    public static float getAngleRadians(double ax, double ay, double bx, double by) {
        return (float) (Math.atan2(by - ay, bx - ax)) + (float) Math.PI;
    }

    /// 根据角度和半径计算点的坐标
    ///
    /// @param radius  半径
    /// @param radians 角度（弧度）
    /// @return Vec2 坐标点
    public static Vec2 pointFromAngle(float radius, float radians) {
        float x = (float) (radius * Math.cos(radians));
        float y = (float) (radius * Math.sin(radians));
        return new Vec2(x, y);
    }

    /// 判断点是否在圆内
    ///
    /// @param point  待检测的点
    /// @param center 圆心
    /// @param radius 半径
    /// @return 如果点在圆内返回true
    public static boolean isPointInCircle(Vec2 point, Vec2 center, float radius) {
        return point.distanceToSqr(center) < radius * radius;
    }

    /// Calc a vector2 that equals to a vector2 rotated an angle
    ///
    /// @param v   origin vector, wont be changed
    /// @param deg angle rotated, in degrees
    /// @return rotated vector2
    public static Vector2f rotationDegrees(Vector2f v, float deg) {
        return rotate(v, deg * Mth.DEG_TO_RAD);
    }

    /// Calc a vector2 that equals to a vector2 rotated an angle
    ///
    /// @param v origin vector, wont be changed
    /// @param d angle rotated, in radians
    /// @return rotated vector2
    public static Vector2f rotate(Vector2f v, float d) {
        return new Vector2f(
                v.x * Mth.cos(d) - v.y * Mth.sin(d),
                v.x * Mth.sin(d) + v.y * Mth.cos(d)
        );
    }

    /// rad
    public static float angle(Vector2f from, Vector2f to) {
        return (float) ((Math.atan2(to.y, to.x) - Math.atan2(from.y, from.x)) % Mth.TWO_PI);
    }

    public static float angleDegrees(Vector2f from, Vector2f to) {
        return angle(from, to) * Mth.RAD_TO_DEG;
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

    /// 计算向量夹角
    ///
    /// @return degree
    public static double angleBetween(Vec3 v1, Vec3 v2) {
        return Math.acos(v1.dot(v2) / v1.length() / v2.length());
    }

    /// 最简单的追踪机制，计算过程如下：
    /// 1. 将当前的弹幕方向进行缩放
    /// 2. 将目标方向（即追踪弹幕想要去的方向）以一定权重加入缩放后的弹幕方向
    /// 3. 调整最终方向向量长度返回
    ///
    /// 优点：计算量少，逻辑简单
    ///
    /// 缺点：在allowLowerSpd为false  且  移动方向与想要弹幕追踪到的方向相反时，追踪能力极为有限；
    ///
    /// interpolateBasis方法以更多的计算量为代价提供更加平滑和可调整的追踪弹道。
    ///
    /// @param currDir            当前的弹幕方向向量
    /// @param targetDir          弹幕追踪目标方向向量
    /// @param currDirScaleFactor 当前弹幕方向计算前的缩放比例
    /// @param homingPower        弹幕追踪时根据目标方向调整的量
    /// @param maxSpeed           弹幕追踪时最大速度; 取值范围 - [0, inf)
    /// @param minSpeed           弹幕追踪时最低速度; 取值范围 - [0, maxSpeed]
    /// @param defaultDir         若更新完毕的弹幕方向为0，但最低速度要求不为0时，返回defaultDir方向（长度会更新为minSpeed）。
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                 *警告*：此向量不要为0
    /// @return 最终更新完毕的方向向量
    public static Vec3 interpolateSimple(
            Vec3 currDir,
            Vec3 targetDir,
            double currDirScaleFactor,
            double homingPower,
            double maxSpeed,
            double minSpeed,
            Vec3 defaultDir
    ) {
        Vec3 result = currDir;
        // 若当前方向和目标方向的方向向量均不为0时才考虑进行方向调整
        // 为了方向的准确性，我们将不考虑长度 < 0.001 (即lengthSqr < 1e-9) 的向量
        if (currDir.lengthSqr() > 1e-9 && targetDir.lengthSqr() > 1e-9) {
            result = currDir.multiply(currDirScaleFactor, currDirScaleFactor, currDirScaleFactor);
            // normalize不会造成NaN，此处长度 > 0.001
            Vec3 targetComponent = targetDir.normalize().multiply(homingPower, homingPower, homingPower);
            result = result.add(targetComponent);
        }
        // 最后，根据最大最小速度的要求更改向量长度
        double vecLen = result.length();
        if (vecLen > maxSpeed) {
            double factor = maxSpeed / vecLen;
            // 不使用normalize以减少一次计算向量长度带来的sqrt运算
            result = result.multiply(factor, factor, factor);
        } else if (vecLen < minSpeed) {
            // 若结果向量过短，使用defaultDir
            if (vecLen < 1e-5) {
                result = defaultDir;
                vecLen = result.length();
            }
            double factor = minSpeed / vecLen;
            // 不使用normalize以减少一次计算向量长度带来的sqrt运算
            result = result.multiply(factor, factor, factor);
        }
        return result;
    }

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

    /// 把向量转成角度
    ///
    /// @return \[yaw, pitch\]
    public static float[] dirToRot(Vec3 vec, boolean toDeg) {
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;
        double h = vec.horizontalDistance();
        float yaw = (float) Mth.atan2(-x, z);
        float pitch = (float) Mth.atan2(-y, h);
        if (toDeg) {
            return new float[]{yaw * Mth.RAD_TO_DEG, pitch * Mth.RAD_TO_DEG};
        }
        return new float[]{yaw, pitch};
    }

    /// 获得从实体A到实体B的单位向量，即A→B
    ///
    /// @param a 实体A
    /// @param b 实体B
    /// @return A→B的单位向量
    public static Vec3 getVectorA2B(Entity a, Entity b) {
        return b.position().subtract(a.position()).normalize();
    }

    public static Direction[] directionsInAxis(Direction.Axis axis) {
        return switch (axis) {
            case X -> new Direction[]{Direction.EAST, Direction.WEST};
            case Y -> new Direction[]{Direction.UP, Direction.DOWN};
            default -> new Direction[]{Direction.SOUTH, Direction.NORTH};
        };
    }

    /// 将输入的向量的某个轴乘一个缩放
    ///
    /// @param vec3  输入的向量
    /// @param axis  某个轴
    /// @param scale 缩放
    /// @return 新向量
    public static Vec3 relativeScale(Vec3 vec3, Direction.Axis axis, double scale) {
        double x = axis == Direction.Axis.X ? scale * vec3.x : vec3.x;
        double y = axis == Direction.Axis.Y ? scale * vec3.y : vec3.y;
        double z = axis == Direction.Axis.Z ? scale * vec3.z : vec3.z;
        return new Vec3(x, y, z);
    }

    public static Vector3f toVector3f(BlockPos pos) {
        return new Vector3f(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
    }

    public static BlockPos fromVector3f(Vector3f vector3d) {
        return new BlockPos(Mth.floor(vector3d.x), Mth.floor(vector3d.y), Mth.floor(vector3d.z));
    }

    /// 计算点到线段的距离
    public static float getDistanceToLineSegment(Vector3f pointA, Vector3f pointB, Vector3f pointP) {
        Vector3f projection = getProjectionOnLineSegment(pointA, pointB, pointP);
        Vector3f distanceVector = new Vector3f(pointP);
        distanceVector.sub(projection);
        return distanceVector.length();
    }

    /// 获得两个位置之间的方向向量；若两点重合则默认返回向上的向量
    ///
    /// 若要自定义默认返回的向量，请在length后传入一个默认向量
    ///
    /// @param start  开始位置的位置向量
    /// @param end    结束位置的位置向量
    /// @param length 返回向量的长度
    public static Vec3 getDirection(Vec3 start, Vec3 end, double length) {
        return getDirection(start, end, length, new Vec3(0, length, 0));
    }

    /// 获得两个位置之间的方向向量；若两点重合则默认返回的向量
    ///
    /// @param start      开始位置的位置向量
    /// @param end        结束位置的位置向量
    /// @param length     返回向量的长度
    /// @param defaultVec 两点重合时返回的默认向量（注：直接原样返回，不会判定该向量的长度）
    public static Vec3 getDirection(Vec3 start, Vec3 end, double length, Vec3 defaultVec) {
        return getDirection(start, end, length, defaultVec, false);
    }

    /// 获得两个位置之间的方向向量
    ///
    /// 若preserveShorterVectors为true且两点之间的距离小于length则不会改变向量长度
    ///
    /// @param start                  开始位置的位置向量
    /// @param end                    结束位置的位置向量
    /// @param length                 返回向量的长度
    /// @param defaultVec             两点重合时返回的默认向量（注：直接原样返回，不会判定该向量的长度）
    /// @param preserveShorterVectors 若向量比length短，是否保留原向量
    public static Vec3 getDirection(Vec3 start, Vec3 end, double length, Vec3 defaultVec, boolean preserveShorterVectors) {
        Vec3 result = end.subtract(start);
        double distSqr = result.lengthSqr();
        // 此时直接返回比length更短的向量
        if (preserveShorterVectors && distSqr <= length * length) {
            return result;
        }
        // 向量长度重设为length

        // 两点之间过近
        if (distSqr < 1e-9) {
            return defaultVec;
        }
        result.scale(length / Math.sqrt(distSqr));
        return result;
    }

    /// 计算点到线段的投影位置
    public static Vector3f getProjectionOnLineSegment(Vector3f pointA, Vector3f pointB, Vector3f pointP) {
        Vector3f direction = new Vector3f(pointB);
        direction.sub(pointA);

        Vector3f pointToP = new Vector3f(pointP);
        pointToP.sub(pointA);

        float dotProduct = pointToP.dot(direction);
        float directionLengthSquared = direction.dot(direction);

        float t = dotProduct / directionLengthSquared;

        Vector3f projection = new Vector3f(direction);
        projection = new Vector3f(projection.x * t, projection.y * t, projection.z * t);
        projection.add(pointA);

        return projection;
    }

    /// 判断垂足是否在线段上
    public static boolean isProjectionBetweenPoints(Vector3f pointA, Vector3f pointB, Vector3f projection) {
        Vector3f point2 = getProjectionOnLineSegment(pointA, pointB, projection);
        float xMax = Math.max(pointA.x, pointB.x) + 0.5F;
        float xMin = Math.min(pointA.x, pointB.x) - 0.5F;
        float yMax = Math.max(pointA.y, pointB.y) + 0.5F;
        float yMin = Math.min(pointA.y, pointB.y) - 0.5F;
        float zMax = Math.max(pointA.z, pointB.z) + 0.5F;
        float zMin = Math.min(pointA.z, pointB.z) - 0.5F;
        return point2.x < xMax && point2.x > xMin && point2.y < yMax && point2.y > yMin && point2.z < zMax && point2.z > zMin;
    }

    /// 计算暴击伤害，如果触发暴击则伤害×1.5
    public static float criticalDamageTotal(float critical, float damage, RandomSource random) {
        return checkChance(critical, random) ? damage * 1.5F : damage;
    }

    public static Vec3 rotToDir(float yRot, float xRot) {
        float cosX = Mth.cos(xRot * Mth.DEG_TO_RAD);
        float x = -Mth.sin(yRot * Mth.DEG_TO_RAD) * cosX;
        float y = -Mth.sin(xRot * Mth.DEG_TO_RAD);
        float z = Mth.cos(yRot * Mth.DEG_TO_RAD) * cosX;
        return new Vec3(x, y, z);
    }
}
