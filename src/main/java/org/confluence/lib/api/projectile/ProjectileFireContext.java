package org.confluence.lib.api.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 一次弹幕动作在服务端捕获的只读上下文。
 *
 * <p>武器栈、视角、眼睛位置、世界与 tick 均来自服务端玩家。普通武器访问器返回防御性副本，
 * 因而动作不能借此绕过成本提交修改玩家背包；真实栈只在统一服务执行成功回调的
 * 最小作用域内开放。</p>
 */
public final class ProjectileFireContext {
    private final ServerPlayer player;
    private final ServerLevel level;
    private final InteractionHand hand;
    private final ItemStack sourceWeapon;
    private final ItemStack weapon;
    private final ProjectileFireTrigger trigger;
    private final Vec3 eyePosition;
    private final Vec3 viewVector;
    private final float yaw;
    private final float pitch;
    private final long gameTime;
    /**
     * 只有统一服务执行成功提交回调时才短暂开放真实栈。
     */
    private boolean successCommitPhase;

    private ProjectileFireContext(
            ServerPlayer player,
            ServerLevel level,
            InteractionHand hand,
            ItemStack weapon,
            ProjectileFireTrigger trigger
    ) {
        this.player = player;
        this.level = level;
        this.hand = hand;
        this.sourceWeapon = weapon;
        this.weapon = weapon.copy();
        this.trigger = trigger;
        this.eyePosition = player.getEyePosition();
        this.viewVector = player.getLookAngle();
        this.yaw = player.getYRot();
        this.pitch = player.getXRot();
        this.gameTime = level.getGameTime();
        requireFinite(eyePosition, "Player eye position");
        requireFinite(viewVector, "Player view vector");
        if (viewVector.lengthSqr() <= 1.0E-12) {
            throw new IllegalArgumentException("Player view vector must not be zero");
        }
    }

    /**
     * 从玩家当前逻辑服务端状态创建上下文。
     */
    public static ProjectileFireContext capture(
            ServerPlayer player,
            InteractionHand hand,
            ProjectileFireTrigger trigger
    ) {
        Objects.requireNonNull(player, "Player must not be null");
        Objects.requireNonNull(hand, "Interaction hand must not be null");
        Objects.requireNonNull(trigger, "Projectile fire trigger must not be null");
        if (!(player.level() instanceof ServerLevel level)) {
            throw new IllegalStateException("Projectile fire context requires a logical server level");
        }
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Projectile fire context must be captured on the server thread");
        }
        ItemStack weapon = player.getItemInHand(hand);
        if (weapon.isEmpty()) {
            throw new IllegalArgumentException("Projectile fire weapon must not be empty");
        }
        return new ProjectileFireContext(player, level, hand, weapon, trigger);
    }

    /**
     * 返回执行动作的服务端玩家。
     */
    public ServerPlayer player() {
        return player;
    }

    /**
     * 返回捕获动作的逻辑服务端世界。
     */
    public ServerLevel level() {
        return level;
    }

    /**
     * 返回服务端重新读取武器的手。
     */
    public InteractionHand hand() {
        return hand;
    }

    /**
     * 返回捕获时手持物的防御性副本。
     */
    public ItemStack weapon() {
        return weapon.copy();
    }

    /**
     * 返回仍位于原手位的动作源武器栈，供成功提交阶段安全修改。
     *
     * <p>返回值只有在当前手持对象仍是捕获动作时的同一个栈实例时才存在。实体加入事件若换手，
     * 调用方会得到 {@code null}，从而不会把 UUID、动画或其他玩法状态写入后来换上的物品。</p>
     */
    public @Nullable ItemStack currentWeaponForCommit() {
        if (!successCommitPhase) return null;
        ItemStack current = player.getItemInHand(hand);
        return current == sourceWeapon ? current : null;
    }

    /**
     * 仅供统一服务在成功动作的最小作用域内开启真实栈访问。
     */
    void beginSuccessCommit() {
        successCommitPhase = true;
    }

    /**
     * 无论成功动作是否抛错，都必须立即关闭真实栈访问。
     */
    void endSuccessCommit() {
        successCommitPhase = false;
    }

    /**
     * 返回本次请求使用的有限触发方式。
     */
    public ProjectileFireTrigger trigger() {
        return trigger;
    }

    /**
     * 返回捕获时的服务端玩家眼睛位置。
     */
    public Vec3 eyePosition() {
        return eyePosition;
    }

    /**
     * 返回服务端视角单位向量。
     */
    public Vec3 viewVector() {
        return viewVector;
    }

    /**
     * 返回捕获时的服务端水平旋转角。
     */
    public float yaw() {
        return yaw;
    }

    /**
     * 返回捕获时的服务端垂直旋转角。
     */
    public float pitch() {
        return pitch;
    }

    /**
     * 返回用于同 tick 幂等门禁的服务端世界时间。
     */
    public long gameTime() {
        return gameTime;
    }

    /**
     * 检查当前手持栈是否仍与捕获时完全一致。
     *
     * <p>该检查在可取消事件后和成本提交前执行，阻止换槽或事件修改武器后重放旧动作。</p>
     */
    public boolean matchesCurrentWeapon() {
        ItemStack current = player.getItemInHand(hand);
        return current == sourceWeapon
                && current.getCount() == weapon.getCount()
                && ItemStack.isSameItemSameTags(current, weapon);
    }

    private static void requireFinite(Vec3 value, String fieldName) {
        if (!Double.isFinite(value.x) || !Double.isFinite(value.y) || !Double.isFinite(value.z)) {
            throw new IllegalArgumentException(fieldName + " must be finite");
        }
    }
}
