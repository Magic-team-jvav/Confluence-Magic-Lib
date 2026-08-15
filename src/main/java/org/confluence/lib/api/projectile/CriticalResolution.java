package org.confluence.lib.api.projectile;

/**
 * 发射时暴击判定的三态结果。
 *
 * <p>{@link #NON_CRITICAL} 表示已经明确判定为不暴击，命中时不得再次抽取；
 * {@link #UNRESOLVED} 表示暴击属性已由外部系统接管，MagicLib 不参与抽取。</p>
 */
public enum CriticalResolution {
    /**
     * MagicLib 未负责本次判定，留给外部扩展事件处理。
     */
    UNRESOLVED,
    /**
     * 发射时已经确定暴击。
     */
    CRITICAL,
    /**
     * 发射时已经确定不暴击。
     */
    NON_CRITICAL;

    /**
     * 返回该结果是否已经在发射时确定。
     */
    public boolean isResolved() {
        return this != UNRESOLVED;
    }

    /**
     * 返回该结果是否明确为暴击。
     */
    public boolean isCritical() {
        return this == CRITICAL;
    }
}
