package org.confluence.lib.api.projectile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 可组合的服务端弹幕资源成本。
 *
 * <p>{@link #prepare(ProjectileFireContext)} 只校验资源并建立可释放的预留，不得提前扣除最终资源；
 * 空结果表示资源不足。统一发射服务完成弹幕预构建和校验后，才会提交实际消耗。</p>
 */
@FunctionalInterface
public interface ProjectileCost {
    /**
     * 校验资源并创建尚未扣除最终资源的预留。
     *
     * @return 已准备成本；资源不足时返回空
     */
    Optional<PreparedProjectileCost> prepare(ProjectileFireContext context);

    /**
     * 返回不消耗资源且永远能够准备成功的成本。
     */
    static ProjectileCost none() {
        return context -> Optional.of(PreparedProjectileCost.none());
    }

    /**
     * 组合多项成本；任一项资源不足或抛出异常时，会按相反顺序释放此前已经建立的预留。
     */
    static ProjectileCost allOf(List<ProjectileCost> costs) {
        Objects.requireNonNull(costs, "Projectile costs must not be null");
        List<ProjectileCost> copy = List.copyOf(costs);
        return context -> {
            Objects.requireNonNull(context, "Projectile fire context must not be null");
            List<PreparedProjectileCost> prepared = new ArrayList<>(copy.size());
            try {
                for (ProjectileCost cost : copy) {
                    Optional<PreparedProjectileCost> result = Objects.requireNonNull(
                            cost.prepare(context), "Projectile cost preparation must not be null");
                    if (result.isEmpty()) {
                        rollbackReverse(prepared);
                        return Optional.empty();
                    }
                    prepared.add(Objects.requireNonNull(
                            result.get(), "Prepared projectile cost must not be null"));
                }
                return Optional.of(PreparedProjectileCost.allOf(prepared));
            } catch (RuntimeException exception) {
                rollbackReverse(prepared);
                throw exception;
            }
        };
    }

    /**
     * 返回先准备当前成本、再准备另一个成本的组合。
     */
    default ProjectileCost and(ProjectileCost other) {
        return allOf(List.of(this, Objects.requireNonNull(other, "Projectile cost must not be null")));
    }

    private static void rollbackReverse(List<PreparedProjectileCost> prepared) {
        RuntimeException failure = null;
        for (int index = prepared.size() - 1; index >= 0; index--) {
            try {
                prepared.get(index).rollback();
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
