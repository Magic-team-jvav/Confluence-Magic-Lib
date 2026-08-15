package org.confluence.lib.api.entity;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * 标记玩家重生保护期间可被清理的实体。
 *
 * <p>默认规则是：玩家重生时，如果附近没有其他存活玩家继续维持遭遇，就丢弃
 * 实现该接口的实体。Boss 可借此在全队阵亡后离场，避免玩家刚复活就被仍停留
 * 在出生点附近的实体再次击杀。</p>
 */
public interface IDiscardWhenRespawnEntity {
    default boolean shouldDiscard(boolean hasNearbyPlayer) {
        return !hasNearbyPlayer;
    }

    static void process(ServerPlayer player) {
        List<Entity> entities = player.level().getEntities(player, player.getBoundingBox().inflate(32));
        boolean hasPlayer = entities.stream().anyMatch(e -> e instanceof Player);
        entities.stream().filter(e -> e instanceof IDiscardWhenRespawnEntity entity && entity.shouldDiscard(hasPlayer)).forEach(Entity::discard);
    }
}
