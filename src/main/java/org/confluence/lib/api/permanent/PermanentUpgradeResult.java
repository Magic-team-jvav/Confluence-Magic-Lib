package org.confluence.lib.api.permanent;

import org.jetbrains.annotations.Nullable;

/**
 * 永久强化尝试的标准结果。
 */
public enum PermanentUpgradeResult {
    APPLIED(null),
    ALREADY_MAXIMUM("message.confluence_magic_lib.permanent_upgrade.already_maximum"),
    ALREADY_MINIMUM("message.confluence_magic_lib.permanent_upgrade.already_minimum"),
    PREREQUISITE_MISSING("message.confluence_magic_lib.permanent_upgrade.prerequisite_missing"),
    EFFECT_REJECTED("message.confluence_magic_lib.permanent_upgrade.effect_rejected");

    private final @Nullable String translationKey;

    PermanentUpgradeResult(@Nullable String translationKey) {
        this.translationKey = translationKey;
    }

    public boolean isApplied() {
        return this == APPLIED;
    }

    public @Nullable String translationKey() {
        return translationKey;
    }
}
