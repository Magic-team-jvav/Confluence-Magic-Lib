package org.confluence.lib.api.permanent;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 面向本体和附属模组的永久强化公共注册表。
 */
public final class PermanentUpgradeRegistry {
    private static final Map<ResourceLocation, PermanentUpgrade> VALUES = new LinkedHashMap<>();

    private PermanentUpgradeRegistry() {}

    /**
     * 注册一个定义；重复 ID 会立即报错。
     */
    public static synchronized PermanentUpgrade register(PermanentUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade");
        PermanentUpgrade previous = VALUES.putIfAbsent(upgrade.id(), upgrade);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate permanent upgrade id: " + upgrade.id());
        }
        return upgrade;
    }

    public static synchronized Optional<PermanentUpgrade> get(ResourceLocation id) {
        return Optional.ofNullable(VALUES.get(id));
    }

    public static synchronized List<PermanentUpgrade> values() {
        return List.copyOf(VALUES.values());
    }

    /**
     * 恢复玩家的全部已注册效果，并返回恢复失败的定义数量。
     */
    public static int reconcileAll(ServerPlayer player) {
        int failures = 0;
        for (PermanentUpgrade upgrade : values()) {
            if (!upgrade.reconcile(player)) failures++;
        }
        return failures;
    }
}
