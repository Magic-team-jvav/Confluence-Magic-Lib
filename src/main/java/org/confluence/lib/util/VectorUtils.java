package org.confluence.lib.util;

import it.unimi.dsi.fastutil.longs.LongArraySet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

public final class VectorUtils {
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
    ///                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           *警告*：此向量不要为0
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

    /// 给予实体B一个击退动量，方向为A→B
    ///
    /// @param a       实体A
    /// @param b       实体B
    /// @param scale   击退动量的缩放
    /// @param motionY 击退的Y轴动量
    public static void knockBackA2B(Entity a, Entity b, double scale, double motionY) {
        if (b instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) scale *= (1.0 - instance.getValue());
        }
        if (scale > 0.0) {
            LivingEntity living = null;
            if (a instanceof TraceableEntity traceable && traceable.getOwner() instanceof LivingEntity living1)
                living = living1;
            else if (a instanceof LivingEntity living1) living = living1;
            if (living != null) {
                AttributeInstance instance = living.getAttribute(Attributes.ATTACK_KNOCKBACK);
                if (instance != null) scale *= (1.0 + instance.getValue());
            }
            b.addDeltaMovement(getVectorA2B(a, b).scale(scale).add(0.0, motionY, 0.0));
        }
    }

    /// 给予实体一个击退动量，方向为vector
    ///
    /// @param attacker 击退者
    /// @param victim   被击退者
    /// @param vector   向量
    public static void knockBack(LivingEntity attacker, Entity victim, Vec3 vector) {
        double scale = 1.0;
        if (victim instanceof LivingEntity living) {
            AttributeInstance instance = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (instance != null) scale *= (1.0 - instance.getValue());
        }
        if (scale > 0.0) {
            LivingEntity living;
            if (attacker instanceof TraceableEntity traceable && traceable.getOwner() instanceof LivingEntity living1)
                living = living1;
            else living = attacker;
            AttributeInstance instance = living.getAttribute(Attributes.ATTACK_KNOCKBACK);
            if (instance != null) scale *= (1.0 + instance.getValue());
            victim.addDeltaMovement(vector.scale(scale));
        }
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

    public static Vector3d toVector3d(BlockPos blockPos) {
        Vec3 center = blockPos.getCenter();
        return new Vector3d(center.x, center.y, center.z);
    }

    public static BlockPos fromVector3d(Vector3d vector3d) {
        return new BlockPos(Mth.floor(vector3d.x), Mth.floor(vector3d.y), Mth.floor(vector3d.z));
    }

    public static void lightningPathList(List<Vector3d> locationList, double dist, float move, RandomSource random) {
        double distSqr = dist * dist;
        boolean refined;
        do {
            refined = false;
            for (int i = 0; i < locationList.size() - 1; i++) {
                Vector3d point1 = locationList.get(i);
                Vector3d point2 = locationList.get(i + 1);
                double distanceSqr = point2.distanceSquared(point1);
                if (distanceSqr > distSqr) {
                    Vector3d midpoint = new Vector3d();
                    point1.add(point2, midpoint).mul(0.5);
                    double offset = Math.sqrt(distanceSqr) * move;
                    double twoOffset = offset * 2;
                    midpoint.x = midpoint.x + (random.nextDouble() - 0.5) * twoOffset;
                    midpoint.y = midpoint.y + (random.nextDouble() - 0.5) * twoOffset;
                    midpoint.z = midpoint.z + (random.nextDouble() - 0.5) * twoOffset;
                    locationList.add(i + 1, midpoint);
                    refined = true;
                }
            }
        } while (refined);
    }

    /**
     * 生成多層次、帶有隨機分支的閃電路徑列表
     *
     * @param initialLocationList 初始的閃電路徑節點列表（起點和終點即可）
     * @param dist                相鄰兩個閃電節點之間的基礎間距（用於細分折線，決定閃電的顆粒度/鋸齒密度）
     * @param move                節點細分時的偏移係數（控制閃電主幹的「抖動」幅度或彎曲程度，值越大越扭曲，超過0.4容易陷入不可控循環，推薦0.125比較寫實）
     * @param random              隨機數生成器實例
     * @param layer               閃電的遞迴/分裂層數（控制分支的層級深度，例如 1 只有主幹分出的一次分支，2 會有二級分支）
     * @param branchPercent       分支長度係數（控制分支相對於主幹的基礎長度比例，結合隨機值決定分支到底有多長）
     * @return 包含所有生成閃電路徑的列表（外層 List 是不同的閃電鏈，內層 List 是單條閃電的軌跡點坐標）
     */
    public static List<List<Vector3d>> lightningPathList(List<Vector3d> initialLocationList, double dist, float move, RandomSource random, int layer, float branchPercent) {
        List<List<Vector3d>> listOfLightning = new ArrayList<>();

        List<List<Vector3d>> currentLayerPaths = new ArrayList<>();
        currentLayerPaths.add(new ArrayList<>(initialLocationList));

        for (int i = 0; i < layer; i++) {
            List<List<Vector3d>> nextLayerPaths = new ArrayList<>();

            for (List<Vector3d> path : currentLayerPaths) {
                List<Vector3d> refinedPath = new ArrayList<>(path);
                lightningPathList(refinedPath, dist, move, random);
                listOfLightning.add(refinedPath);

                if (refinedPath.size() < 2) continue;

                Vector3d vctBefore = refinedPath.getFirst();
                Vector3d lastVct = refinedPath.getLast();

                for (int j = 1; j < refinedPath.size(); j++) {
                    Vector3d vct = refinedPath.get(j);

                    double percent = 0.02 * (1 - ((double) j / refinedPath.size()));
                    if (random.nextDouble() < percent) {
                        Vector3d branchDirection = new Vector3d(vct).sub(vctBefore).normalize().mul(vctBefore.distance(lastVct) * (branchPercent + random.nextDouble() * 0.1));

                        Vector3d randomOffset = new Vector3d(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5)
                                .normalize().mul(branchDirection.length() * 0.5);

                        Vector3d branchEnd = new Vector3d(vctBefore).add(branchDirection).add(randomOffset);

                        List<Vector3d> newBranch = new ArrayList<>();
                        newBranch.add(new Vector3d(vctBefore));
                        newBranch.add(branchEnd);
                        nextLayerPaths.add(newBranch);
                    }
                    vctBefore = vct;
                }
            }
            currentLayerPaths = nextLayerPaths;
            if (currentLayerPaths.isEmpty()) {
                break;
            }
        }

        return listOfLightning;
    }

    public static Map<Vector3d, BooleanStorage4> mazePos(Vector3d centerPos, double distance, int layer, RandomSource random, float difficulty) {
        Map<Vector3i, BooleanStorage4> nowMap = new HashMap<>();
        Map<Vector3i, BooleanStorage4> thanMap = new HashMap<>();
        Map<Vector3i, BooleanStorage4> setMap = new HashMap<>();
        Map<Vector3d, BooleanStorage4> outMap = new HashMap<>();
        thanMap.put(new Vector3i(), new BooleanStorage4());
        int maxCount = Mth.square(layer * 2 + 1);
        while (setMap.size() < maxCount) {
            nowMap.clear();
            nowMap.putAll(thanMap);
            nowMap.putAll(setMap);
            thanMap.clear();
            for (Map.Entry<Vector3i, BooleanStorage4> entry : nowMap.entrySet()) {
                Vector3i key = entry.getKey();
                int x = key.x;
                int z = key.z;
                BooleanStorage4 a = entry.getValue().copy();
                BooleanStorage4 b = a.copy();
                for (int i = 0; i < 4; i++) {
                    int xOffset = (int) Mth.cos(i * Mth.HALF_PI) + x;
                    int zOffset = (int) Mth.sin(i * Mth.HALF_PI) + z;
                    Vector3i thanKey = new Vector3i(xOffset, 0, zOffset);
                    b.set(i, true);
                    if (((1.0F - 0.5F * difficulty) > random.nextFloat()) && (xOffset <= layer) && (xOffset >= -layer) && (zOffset <= layer) && (zOffset >= -layer) && !setMap.containsKey(thanKey) && !nowMap.containsKey(thanKey) && !thanMap.containsKey(thanKey)) {
                        a.set(i, true);
                        BooleanStorage4 thenList = new BooleanStorage4();
                        thenList.set((i + 2) % 4, true);
                        thanMap.put(thanKey, thenList);
                    }
                }
                setMap.put(key, a);
            }
        }
        for (Map.Entry<Vector3i, BooleanStorage4> entry : setMap.entrySet()) {
            Vector3i key = entry.getKey();
            int x = key.x;
            int z = key.z;
            double dX = x * distance + centerPos.x;
            double dZ = z * distance + centerPos.z;
            BooleanStorage4 outList = entry.getValue().copy();
            outMap.put(new Vector3d(dX, centerPos.y, dZ), outList);
        }
        return outMap;
    }

    public static int listRandom(BooleanStorage4 list, RandomSource random) {
        for (int i = 0; i < 100; i++) {
            int listW = random.nextInt(list.size());
            if (!list.get(listW)) {
                return listW;
            }
        }
        return 0;
    }

    public static void list8(List<Vector3d> list, BlockPos centerPos, int x, int y, int z, RandomSource random) {
        for (int i = 0; i < 8; i++) {
            if (!LibMathUtils.checkChance(0.125F, random)) continue;
            list.add(new Vector3d(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1))));
        }
    }

    public static void list8(List<Vector3d> list, BlockPos centerPos, int x, int y, int z, RandomSource random, int checkY) {
        for (int i = 0; i < 8; i++) {
            if (!LibMathUtils.checkChance(0.125F, random)) continue;
            Vector3d pos = new Vector3d(centerPos.getX() + (x * ((i < 4) ? 1 : -1)), centerPos.getY() + (y * ((i % 4 < 2) ? 1 : -1)), centerPos.getZ() + (z * ((i % 2 < 1) ? 1 : -1)));
            if (pos.y < checkY) {
                list.add(pos);
            }
        }
    }

    /// 计算点到线段的投影位置
    public static Vector3d getProjectionOnLineSegment(Vector3d pointA, Vector3d pointB, Vector3d pointP) {
        Vector3d direction = new Vector3d(pointB);
        direction.sub(pointA);

        Vector3d pointToP = new Vector3d(pointP);
        pointToP.sub(pointA);

        double dotProduct = pointToP.dot(direction);
        double directionLengthSquared = direction.dot(direction);

        double t = dotProduct / directionLengthSquared;

        Vector3d projection = new Vector3d(direction);
        projection = new Vector3d(projection.x * t, projection.y * t, projection.z * t);
        projection.add(pointA);

        return projection;
    }

    /// 计算点到线段的距离
    public static double getDistanceToLineSegment(Vector3d pointA, Vector3d pointB, Vector3d pointP) {
        Vector3d projection = getProjectionOnLineSegment(pointA, pointB, pointP);
        Vector3d distanceVector = new Vector3d(pointP);
        distanceVector.sub(projection);
        return distanceVector.length();
    }

    /// 判断垂足是否在线段上
    public static boolean isProjectionBetweenPoints(Vector3d pointA, Vector3d pointB, Vector3d projection) {
        Vector3d point2 = getProjectionOnLineSegment(pointA, pointB, projection);
        double xMax = Math.max(pointA.x, pointB.x) + 0.5;
        double xMin = Math.min(pointA.x, pointB.x) - 0.5;
        double yMax = Math.max(pointA.y, pointB.y) + 0.5;
        double yMin = Math.min(pointA.y, pointB.y) - 0.5;
        double zMax = Math.max(pointA.z, pointB.z) + 0.5;
        double zMin = Math.min(pointA.z, pointB.z) - 0.5;
        return point2.x < xMax && point2.x > xMin && point2.y < yMax && point2.y > yMin && point2.z < zMax && point2.z > zMin;
    }

    /// 生成坐标列表
    ///
    /// 生成球体坐标列表，带有随机比例
    public static List<Vector3d> ballPos(double radiusD, BlockPos centerPos, float chance, RandomSource random) {
        List<Vector3d> list = new ArrayList<>();
        int radius = (int) radiusD + 1;
        double radius2 = radiusD * radiusD;
        int x2;
        int y2;
        float chance8 = chance * 8;
        for (int x = 0; x < radius; x++) {
            x2 = x * x;
            for (int y = 0; y < radius; y++) {
                y2 = y * y;
                for (int z = 0; z < radius; z++) {
                    if (LibMathUtils.checkChance(chance8, random) && x2 + y2 + z * z <= radius2) {
                        list8(list, centerPos, x, y, z, random);
                    }
                }
            }
        }
        return list;
    }

    /// 生成椭球体坐标列表，带有随机比例
    public static List<Vector3d> ellipsoidPos(double radiusDX, double radiusDY, double radiusDZ, BlockPos centerPos, float chance, RandomSource random) {
        List<Vector3d> list = new ArrayList<>();
        int radiusX = Mth.ceil(radiusDX);
        int radiusY = Mth.ceil(radiusDY);
        int radiusZ = Mth.ceil(radiusDZ);
        double inv_rX = LibMathUtils.invertSquare(radiusDX);
        double inv_rY = LibMathUtils.invertSquare(radiusDY);
        double inv_rZ = LibMathUtils.invertSquare(radiusDZ);
        float chance8 = chance * 8;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    if (LibMathUtils.checkChance(chance8, random) && x2 * inv_rX + y2 * inv_rY + z * z * inv_rZ <= 1) {
                        list8(list, centerPos, x, y, z, random);
                    }
                }
            }
        }
        return list;
    }

    /// 生成椭球体坐标列表，带有内径、随机比例、最大y坐标
    public static List<Vector3d> ellipsoidPos(double radiusDXIn, double radiusDYIn, double radiusDZIn, double radiusDXOut, double radiusDYOut, double radiusDZOut, BlockPos centerPos, float chance, RandomSource random, int checkY) {
        List<Vector3d> list = new ArrayList<>();
        int radiusX = Mth.ceil(radiusDXOut);
        int radiusY = Mth.ceil(radiusDYOut);
        int radiusZ = Mth.ceil(radiusDZOut);
        double inv_rXOut = LibMathUtils.invertSquare(radiusDXOut);
        double inv_rYOut = LibMathUtils.invertSquare(radiusDYOut);
        double inv_rZOut = LibMathUtils.invertSquare(radiusDZOut);
        double inv_rXIn = LibMathUtils.invertSquare(radiusDXIn);
        double inv_rYIn = LibMathUtils.invertSquare(radiusDYIn);
        double inv_rZIn = LibMathUtils.invertSquare(radiusDZIn);
        float chance8 = chance * 8;
        for (int x = 0; x < radiusX; x++) {
            int x2 = x * x;
            for (int y = 0; y < radiusY; y++) {
                int y2 = y * y;
                for (int z = 0; z < radiusZ; z++) {
                    int z2 = z * z;
                    if (LibMathUtils.checkChance(chance8, random) &&
                            x2 * inv_rXOut + y2 * inv_rYOut + z2 * inv_rZOut <= 1 &&
                            x2 * inv_rXIn + y2 * inv_rYIn + z2 * inv_rZIn >= 1
                    ) {
                        list8(list, centerPos, x, y, z, random, checkY);
                    }
                }
            }
        }
        return list;
    }

    /// 生成螺旋形坐标列表
    public static List<Vector3d> rotateCloudPos(float rotate, float rotateStep, double length, double lengthStep, int count, BlockPos centerPos) {
        List<Vector3d> poses = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            poses.add(new Vector3d(
                    centerPos.getX() + (length + lengthStep * i) * Mth.cos(rotate + rotateStep * i),
                    centerPos.getY(),
                    centerPos.getZ() + (length + lengthStep * i) * Mth.sin(rotate + rotateStep * i)
            ));
        }
        return poses;
    }

    /// 生成环形坐标列表
    public static void roundPos(BlockPos centerPos, double radius, RandomSource random, List<Vector3d> list, int offset, int rotate, float start) {
        float rStep = Mth.TWO_PI / rotate;
        for (int i = 0; i < rotate; i++) {
            list.add(VectorUtils.toVector3d(centerPos.offset(
                    Mth.floor(Mth.cos(rStep * i + start) * radius) + random.nextInt(-offset, offset + 1),
                    0,
                    Mth.floor(Mth.sin(rStep * i + start) * radius) + random.nextInt(-offset, offset + 1)
            )));
        }
    }

    /// 生成任意角度圆台坐标列表
    public static List<Vector3d> frustumSetPos(Vector3d startPos, Vector3d endPos, double startRadius, double endRadius, float chance, RandomSource random) {
        int xStart0 = (int) (startPos.x + startRadius + 1);
        int xStart1 = (int) (startPos.x - startRadius - 1);
        int xEnd0 = (int) (endPos.x + endRadius + 1);
        int xEnd1 = (int) (endPos.x - endRadius - 1);
        int yStart0 = (int) (startPos.y + startRadius + 1);
        int yStart1 = (int) (startPos.y - startRadius - 1);
        int yEnd0 = (int) (endPos.y + endRadius + 1);
        int yEnd1 = (int) (endPos.y - endRadius - 1);
        int zStart0 = (int) (startPos.z + startRadius + 1);
        int zStart1 = (int) (startPos.z - startRadius - 1);
        int zEnd0 = (int) (endPos.z + endRadius + 1);
        int zEnd1 = (int) (endPos.z - endRadius - 1);

        int setStartX = Math.min(xStart1, xEnd1);
        int setEndX = Math.max(xStart0, xEnd0);
        int setStartY = Math.min(yStart1, yEnd1);
        int setEndY = Math.max(yStart0, yEnd0);
        int setStartZ = Math.min(zStart1, zEnd1);
        int setEndZ = Math.max(zStart0, zEnd0);

        double inv_length = 1 / startPos.distance(endPos);

        List<Vector3d> list = new ArrayList<>();

        for (int x = setStartX; x <= setEndX; x++) {
            for (int y = setStartY; y <= setEndY; y++) {
                for (int z = setStartZ; z <= setEndZ; z++) {
                    if (chance > random.nextFloat()) {
                        Vector3d pointP = new Vector3d(x, y, z);
                        if (!isProjectionBetweenPoints(startPos, endPos, pointP)) continue;
                        Vector3d pointP2 = getProjectionOnLineSegment(startPos, endPos, pointP);
                        double lengthGet = pointP2.distance(endPos);//0;//Math.sqrt(y2 + Mth.square(endPos.z - z) - getDistanceToLineSegment(startPos, endPos, pointP));
                        double lengthP = lengthGet * inv_length;
                        if (pointP.distance(pointP2) <= startRadius * lengthP + endRadius * (1.0D - lengthP)) {
                            list.add(pointP);
                        }
                    }
                }
            }
        }
        return list;
    }

    /// 立方体坐标列表，带有随机比例
    public static List<Vector3d> rectangularPos(BlockPos startPos, BlockPos endPos, float chance, RandomSource random) {
        int startX = Math.min(endPos.getX(), startPos.getX());
        int startY = Math.min(endPos.getY(), startPos.getY());
        int startZ = Math.min(endPos.getZ(), startPos.getZ());
        int endX = Math.max(endPos.getX(), startPos.getX());
        int endY = Math.max(endPos.getY(), startPos.getY());
        int endZ = Math.max(endPos.getZ(), startPos.getZ());
        int xLength = endX - startX;
        int yLength = endY - startY;
        int zLength = endZ - startZ;
        List<Vector3d> list = new ArrayList<>();
        for (int x = 0; x <= xLength; x++) {
            for (int y = 0; y <= yLength; y++) {
                for (int z = 0; z <= zLength; z++) {
                    if (!LibMathUtils.checkChance(chance, random)) continue;
                    list.add(new Vector3d(startX + x, startY + y, startZ + z));
                }
            }
        }
        return list;
    }

    public static void findVerticalPlane(Vector3d point, Vector3d before, Vector3d after, double side, List<Vector3d> returnList) {
        double l = point.distance(after) / point.distance(before);

        double hSide = side * 0.5;

        double x0 = ((point.x - before.x) * l + (after.x - point.x)) * 0.5;
        double y0 = ((point.y - before.y) * l + (after.y - point.y)) * 0.5;
        double z0 = ((point.z - before.z) * l + (after.z - point.z)) * 0.5;

        double[] nX = new double[3];
        double[] nZ = new double[3];
        double lToY2 = Mth.length(x0, y0);

        if (x0 != 0) {
            nX = new double[]{y0 / lToY2, -x0 / lToY2, 0};
            double inv_lX = 1 / LibMathUtils.length(nX);
            nX[0] = nX[0] * inv_lX;
            nX[1] = nX[1] * inv_lX;
            nX[2] = nX[2] * inv_lX;
        }
        if (z0 != 0) {
            double inv_l2 = 1 / Mth.length(x0, y0, z0);
            nZ = new double[]{-z0 * x0 * inv_l2 / lToY2, -z0 * y0 * inv_l2 / lToY2, lToY2 * inv_l2};
            double inv_lZ = 1 / LibMathUtils.length(nZ);
            nZ[0] *= inv_lZ;
            nZ[1] *= inv_lZ;
            nZ[2] *= inv_lZ;
        }

        returnList.add(new Vector3d((nX[0] + nZ[0]) * hSide + point.x, (nX[1] + nZ[1]) * hSide + point.y, (nX[2] + nZ[2]) * hSide + point.z));
        returnList.add(new Vector3d((nX[0] - nZ[0]) * hSide + point.x, (nX[1] - nZ[1]) * hSide + point.y, (nX[2] - nZ[2]) * hSide + point.z));
        returnList.add(new Vector3d((-nX[0] - nZ[0]) * hSide + point.x, (-nX[1] - nZ[1]) * hSide + point.y, (-nX[2] - nZ[2]) * hSide + point.z));
        returnList.add(new Vector3d((-nX[0] + nZ[0]) * hSide + point.x, (-nX[1] + nZ[1]) * hSide + point.y, (-nX[2] + nZ[2]) * hSide + point.z));
    }

    /// 凸包的内部點集采樣
    public static List<BlockPos> getBlocksInConvexHull(@Nullable List<Vector3d> points) {
        List<BlockPos> result = new ArrayList<>();
        if (points == null || points.size() < 4) return result;

        double cx = 0, cy = 0, cz = 0;
        for (Vector3d p : points) {
            cx += p.x;
            cy += p.y;
            cz += p.z;
        }
        double inv_size = 1.0 / points.size();
        cx *= inv_size;
        cy *= inv_size;
        cz *= inv_size;

        int pivotIdx = 0;
        double maxDistSq = 0;
        for (int i = 0; i < points.size(); i++) {
            double d = points.get(i).distanceSquared(cx, cy, cz);
            if (d > maxDistSq) {
                maxDistSq = d;
                pivotIdx = i;
            }
        }
        Vector3d pivot = points.get(pivotIdx);

        int p1Idx = -1;
        maxDistSq = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx) continue;
            double d = points.get(i).distanceSquared(pivot);
            if (d > maxDistSq) {
                maxDistSq = d;
                p1Idx = i;
            }
        }
        if (p1Idx == -1) return result;
        Vector3d p1 = points.get(p1Idx);

        int p2Idx = -1;
        double maxArea = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx) continue;
            double area = triArea(pivot, p1, points.get(i));
            if (area > maxArea) {
                maxArea = area;
                p2Idx = i;
            }
        }
        if (p2Idx == -1) return result;
        Vector3d p2 = points.get(p2Idx);

        int p3Idx = -1;
        double maxDist = 0;
        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx || i == p2Idx) continue;
            double d = pointToPlaneDist(points.get(i), pivot, p1, p2);
            if (Math.abs(d) > Math.abs(maxDist)) {
                maxDist = d;
                p3Idx = i;
            }
        }
        if (p3Idx == -1) return result;
        if (Math.abs(maxDist) < 1e-7) return result;
        Vector3d p3 = points.get(p3Idx);

        cx = (pivot.x + p1.x + p2.x + p3.x) * 0.25;
        cy = (pivot.y + p1.y + p2.y + p3.y) * 0.25;
        cz = (pivot.z + p1.z + p2.z + p3.z) * 0.25;

        int[][] faces = maxDist > 0 ? new int[][]{
                {pivotIdx, p1Idx, p2Idx},
                {pivotIdx, p2Idx, p3Idx},
                {pivotIdx, p3Idx, p1Idx},
                {p1Idx, p3Idx, p2Idx}
        } : new int[][]{
                {pivotIdx, p2Idx, p1Idx},
                {pivotIdx, p1Idx, p3Idx},
                {pivotIdx, p3Idx, p2Idx},
                {p1Idx, p2Idx, p3Idx}
        };

        for (int i = 0; i < points.size(); i++) {
            if (i == pivotIdx || i == p1Idx || i == p2Idx || i == p3Idx) continue;

            Vector3d point = points.get(i);
            List<int[]> newFaces = new ArrayList<>();
            LongSet boundaryEdges = new LongArraySet();

            for (int[] face : faces) {
                if (isPointOutside(point, points.get(face[0]), points.get(face[1]), points.get(face[2]), cx, cy, cz)) {
                    addEdge(boundaryEdges, face[0], face[1]);
                    addEdge(boundaryEdges, face[1], face[2]);
                    addEdge(boundaryEdges, face[2], face[0]);
                } else {
                    newFaces.add(face);
                }
            }

            if (boundaryEdges.isEmpty()) continue;

            for (long edge : boundaryEdges) {
                int a = (int) (edge >> 32);
                int b = (int) edge;
                newFaces.add(new int[]{a, b, i});
            }
            faces = newFaces.toArray(int[][]::new);
        }

        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (Vector3d p : points) {
            minX = Math.min(minX, p.x);
            maxX = Math.max(maxX, p.x);
            minY = Math.min(minY, p.y);
            maxY = Math.max(maxY, p.y);
            minZ = Math.min(minZ, p.z);
            maxZ = Math.max(maxZ, p.z);
        }

        BlockPos.MutableBlockPos mPos = new BlockPos.MutableBlockPos();
        int ceilX = Mth.ceil(maxX);
        int ceilY = Mth.ceil(maxY);
        int ceilZ = Mth.ceil(maxZ);
        for (int x = Mth.floor(minX); x <= ceilX; x++) {
            mPos.setX(x);
            for (int y = Mth.floor(minY); y <= ceilY; y++) {
                mPos.setY(y);
                label:
                for (int z = Mth.floor(minZ); z <= ceilZ; z++) {
                    double px = x + 0.5, py = y + 0.5, pz = z + 0.5;
                    for (int[] face : faces) {
                        Vector3d a = points.get(face[0]), b = points.get(face[1]), c = points.get(face[2]);

                        double[] normal = getNormal(a, b, c);
                        double nx = normal[0], ny = normal[1], nz = normal[2];

                        double fcx = (a.x + b.x + c.x) * 0.3333333;
                        double fcy = (a.y + b.y + c.y) * 0.3333333;
                        double fcz = (a.z + b.z + c.z) * 0.3333333;
                        if (nx * (cx - fcx) + ny * (cy - fcy) + nz * (cz - fcz) > 0) {
                            nx = -nx;
                            ny = -ny;
                            nz = -nz;
                        }

                        if ((px - a.x) * nx + (py - a.y) * ny + (pz - a.z) * nz > 1e-7) {
                            continue label;
                        }
                    }
                    result.add(mPos.setZ(z).immutable());
                }
            }
        }
        return result;
    }

    private static double[] getNormal(Vector3d a, Vector3d b, Vector3d c) {
        double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        double nx = aby * acz - abz * acy;
        double ny = abz * acx - abx * acz;
        double nz = abx * acy - aby * acx;
        return new double[]{nx, ny, nz};
    }

    private static void addEdge(LongSet boundary, int a, int b) {
        long reverseCode = ((long) b << 32) | a;

        if (boundary.contains(reverseCode)) {
            boundary.remove(reverseCode);
        } else {
            boundary.add(((long) a << 32) | b);
        }
    }

    private static double triArea(Vector3d a, Vector3d b, Vector3d c) {
        double abx = b.x - a.x, aby = b.y - a.y, abz = b.z - a.z;
        double acx = c.x - a.x, acy = c.y - a.y, acz = c.z - a.z;
        double nx = aby * acz - abz * acy;
        double ny = abz * acx - abx * acz;
        double nz = abx * acy - aby * acx;
        return Mth.length(nx, ny, nz);
    }

    private static double pointToPlaneDist(Vector3d p, Vector3d a, Vector3d b, Vector3d c) {
        double[] normal = getNormal(a, b, c);
        return (p.x - a.x) * normal[0] + (p.y - a.y) * normal[1] + (p.z - a.z) * normal[2];
    }

    private static boolean isPointOutside(Vector3d p, Vector3d a, Vector3d b, Vector3d c, double cx, double cy, double cz) {
        double[] normal = getNormal(a, b, c);
        double nx = normal[0], ny = normal[1], nz = normal[2];

        double fcx = (a.x + b.x + c.x) * 0.3333333;
        double fcy = (a.y + b.y + c.y) * 0.3333333;
        double fcz = (a.z + b.z + c.z) * 0.3333333;

        if (nx * (cx - fcx) + ny * (cy - fcy) + nz * (cz - fcz) > 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }

        return (p.x - a.x) * nx + (p.y - a.y) * ny + (p.z - a.z) * nz > 1e-7;
    }
}
