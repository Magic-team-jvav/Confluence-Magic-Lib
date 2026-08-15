package org.confluence.lib.api.permanent;

import net.minecraft.server.level.ServerPlayer;

/**
 * 传递给前置条件与效果的不可变上下文。
 *
 * @param player        服务端权威玩家
 * @param previousLevel 事务前等级
 * @param level         本次希望投影或正在恢复的等级；回溯定义允许为负数
 * @param restoring     是否为登录/重生后的幂等效果重建；恢复时不得无标记重复发奖，一次性奖励应使用持久完成标记
 */
public record PermanentUpgradeContext(ServerPlayer player, int previousLevel, int level,
                                      boolean restoring) {}
