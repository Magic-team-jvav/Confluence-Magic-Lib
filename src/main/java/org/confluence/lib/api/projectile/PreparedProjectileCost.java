package org.confluence.lib.api.projectile;

import java.util.List;
import java.util.Objects;

/**
 * 已完成校验和预留、尚未提交的弹幕动作成本。
 *
 * <p>{@link #commit()} 与 {@link #rollback()} 都保证最多执行一次；回滚既可释放尚未提交的预留，
 * 也可补偿已提交资源。实现者提供的回滚动作必须同时覆盖这两种状态并保持精确幂等，不能依赖
 * 第二次调用修复第一次调用。</p>
 */
public final class PreparedProjectileCost {
    private enum State {
        PREPARED,
        COMMITTED,
        ROLLED_BACK
    }

    private final Runnable commitAction;
    private final Runnable rollbackAction;
    private State state = State.PREPARED;

    private PreparedProjectileCost(Runnable commitAction, Runnable rollbackAction) {
        this.commitAction = Objects.requireNonNull(commitAction, "Commit action must not be null");
        this.rollbackAction = Objects.requireNonNull(rollbackAction, "Rollback action must not be null");
    }

    /**
     * 创建保证精确一次调用语义的成本。
     */
    public static PreparedProjectileCost once(Runnable commit, Runnable rollback) {
        return new PreparedProjectileCost(commit, rollback);
    }

    /**
     * 创建每次调用均独立、无需资源的空成本。
     */
    public static PreparedProjectileCost none() {
        return once(() -> {}, () -> {});
    }

    /**
     * 按声明顺序提交、按相反顺序回滚一组已准备成本。
     *
     * <p>集合会立即复制；空集合等价于 {@link #none()}。</p>
     */
    public static PreparedProjectileCost allOf(List<PreparedProjectileCost> costs) {
        Objects.requireNonNull(costs, "Prepared costs must not be null");
        List<PreparedProjectileCost> copy = List.copyOf(costs);
        return once(
                () -> copy.forEach(PreparedProjectileCost::commit),
                () -> rollbackAll(copy)
        );
    }

    /**
     * 提交一次；已经提交或回滚后再次调用不会产生副作用。
     */
    public void commit() {
        if (state == State.PREPARED) {
            state = State.COMMITTED;
            commitAction.run();
        }
    }

    /**
     * 回滚或释放预留一次；之后提交与重复回滚均不会产生副作用。
     */
    public void rollback() {
        if (state == State.ROLLED_BACK) return;
        state = State.ROLLED_BACK;
        rollbackAction.run();
    }

    /**
     * 返回成本是否已经成功进入提交状态且尚未回滚。
     */
    public boolean isCommitted() {
        return state == State.COMMITTED;
    }

    /**
     * 返回成本是否已经执行或尝试执行过回滚。
     */
    public boolean isRolledBack() {
        return state == State.ROLLED_BACK;
    }

    private static void rollbackAll(List<PreparedProjectileCost> costs) {
        RuntimeException failure = null;
        for (int index = costs.size() - 1; index >= 0; index--) {
            try {
                costs.get(index).rollback();
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = exception;
                } else {
                    failure.addSuppressed(exception);
                }
            }
        }
        if (failure != null) throw failure;
    }
}
