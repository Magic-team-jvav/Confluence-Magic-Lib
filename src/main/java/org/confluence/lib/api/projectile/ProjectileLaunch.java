package org.confluence.lib.api.projectile;

import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/**
 * 一个尚未加入世界的弹幕及其服务端生成位置和方向。
 *
 * <p>方向会在构造时归一化；实际速度由战斗快照统一提供。实体仍是可变的 Minecraft 对象，
 * 但在统一发射服务提交前，不得由布局或动作自行加入世界。</p>
 */
public final class ProjectileLaunch {
    private final Projectile projectile;
    private final Vec3 position;
    private final Vec3 direction;
    private final float velocityMultiplier;

    public ProjectileLaunch(Projectile projectile, Vec3 position, Vec3 direction) {
        this(projectile, position, direction, 1.0F);
    }

    /**
     * 创建一项带独立速度倍率的发射描述。
     *
     * <p>倍率只调整本枚弹幕的最终速度，不会重新解析武器属性。零倍率用于拖拽法术等“先生成、
     * 后释放”的实体；方向仍必须有效，方便实体稍后恢复同一服务端视角。</p>
     */
    public ProjectileLaunch(
            Projectile projectile,
            Vec3 position,
            Vec3 direction,
            float velocityMultiplier
    ) {
        this.projectile = Objects.requireNonNull(projectile, "Projectile must not be null");
        this.position = requireFinite(Objects.requireNonNull(position, "Launch position must not be null"),
                "Launch position");
        Vec3 checkedDirection = requireFinite(
                Objects.requireNonNull(direction, "Launch direction must not be null"), "Launch direction");
        if (checkedDirection.lengthSqr() <= 1.0E-12) {
            throw new IllegalArgumentException("Launch direction must not be zero");
        }
        this.direction = checkedDirection.normalize();
        if (!Float.isFinite(velocityMultiplier) || velocityMultiplier < 0.0F) {
            throw new IllegalArgumentException("Launch velocity multiplier must be finite and non-negative");
        }
        this.velocityMultiplier = velocityMultiplier;
    }

    /**
     * 返回尚未加入世界的可变实体；只允许统一发射服务继续初始化它。
     */
    public Projectile projectile() {
        return projectile;
    }

    /**
     * 返回服务端生成位置。
     */
    public Vec3 position() {
        return position;
    }

    /**
     * 返回已经归一化的服务端发射方向。
     */
    public Vec3 direction() {
        return direction;
    }

    /**
     * 返回相对于战斗快照弹速的单枚倍率。
     */
    public float velocityMultiplier() {
        return velocityMultiplier;
    }

    private static Vec3 requireFinite(Vec3 value, String fieldName) {
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
        return value;
    }
}
