package org.confluence.lib.api.entity;

import com.google.common.collect.Streams;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.confluence.lib.color.GlobalColors;

/**
 * 标记 Boss 主体及其从属部件的通用接口。
 *
 * <p>本接口只描述跨模块都需要理解的 Boss 语义：实体是否显示全局提示、
 * 是否为 Boss 主体，以及多人战斗时是否允许实现模块按参与人数强化。
 * 具体的血条、脱战、掉落、多人倍率和部件生命周期由实现模块负责。</p>
 */
public interface Boss extends Enemy, IDiscardWhenRespawnEntity {
    /**
     * 是否发送出生和死亡消息。Boss 部件默认不发送。
     */
    default boolean shouldShowMessage() {
        return isMainBody();
    }

    /**
     * 是否为代表整场战斗的 Boss 主体。
     */
    default boolean isMainBody() {
        return true;
    }

    /**
     * 是否允许实现模块按游戏难度和玩家数量强化该 Boss。
     *
     * <p>这个方法只提供开关，不规定具体倍率。实现模块应在实体生成阶段
     * 读取它，并保证属性修饰符不会被重复应用。</p>
     */
    default boolean shouldEnhanceMultiplayer() {
        return true;
    }

    /**
     * Boss 的从属部件不代表整场战斗，因此不独立播报。
     */
    interface BossPart extends Boss {
        @Override
        default boolean isMainBody() {
            return false;
        }
    }

    /**
     * 向 Boss 所在维度的所有玩家发送苏醒消息。
     */
    static void sendBossSpawnMessage(Entity entity) {
        Level level = entity.level();
        if (!level.isClientSide && entity instanceof Boss boss
                && boss.shouldShowMessage()) {
            Component message = Component.translatable(
                            "message.confluence.boss_spawn",
                            entity.getDisplayName())
                    .withColor(GlobalColors.EVENT.get())
                    .withStyle(ChatFormatting.BOLD);
            for (Player player : level.players()) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * 向 Boss 所在维度的所有玩家发送击败消息。
     */
    static void sendBossDeathMessage(Entity entity) {
        Level level = entity.level();
        if (!level.isClientSide && entity instanceof Boss boss
                && boss.shouldShowMessage()) {
            Component message = Component.translatable(
                            "message.confluence.boss_leave",
                            entity.getDisplayName())
                    .withColor(GlobalColors.EVENT.get())
                    .withStyle(ChatFormatting.BOLD);
            for (Player player : level.players()) {
                player.sendSystemMessage(message);
            }
        }
    }

    /**
     * 判断当前维度是否不存在任何 Boss 主体或部件。
     */
    static boolean noBossInWorld(ServerLevel level) {
        return Streams.stream(level.getAllEntities())
                .noneMatch(entity -> entity instanceof Boss);
    }
}
