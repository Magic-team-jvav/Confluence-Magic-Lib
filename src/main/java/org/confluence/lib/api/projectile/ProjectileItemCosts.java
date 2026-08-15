package org.confluence.lib.api.projectile;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;
import java.util.Optional;

/**
 * 弹幕发射流程可复用的手持物成本工厂。
 *
 * <p>该类只负责服务端手持栈的准备、精确一次提交和补偿回滚，不了解具体武器、弹幕或模组规则。
 * 附属模组可以直接把返回值交给 {@link ProjectileFireAction}，无需重复实现创造模式、事件换栈和
 * 生成失败退款等容易出错的边界。</p>
 */
public final class ProjectileItemCosts {
    private ProjectileItemCosts() {}

    /**
     * 返回消耗一份当前手持物的成本；创造模式自动使用空成本。
     */
    public static ProjectileCost oneHeldItem() {
        return heldItems(1);
    }

    /**
     * 返回消耗指定数量当前手持物的成本；数量在创建成本时立即校验。
     *
     * <p>准备阶段捕获手持栈身份和完整物品组件，提交时再次核对。若实体生成监听器在提交后替换
     * 手持栈，回滚会向背包返还独立副本，不会增长已经脱离玩家槽位的旧对象。</p>
     */
    public static ProjectileCost heldItems(int amount) {
        if (amount < 1) {
            throw new IllegalArgumentException("Held item cost must be positive");
        }
        return context -> prepareHeldItems(context, amount);
    }

    private static Optional<PreparedProjectileCost> prepareHeldItems(
            ProjectileFireContext context,
            int amount
    ) {
        Objects.requireNonNull(context, "Projectile fire context must not be null");
        ServerPlayer player = context.player();
        if (player.hasInfiniteMaterials()) {
            return Optional.of(PreparedProjectileCost.none());
        }

        ItemStack chargedStack = player.getItemInHand(context.hand());
        if (chargedStack.isEmpty() || chargedStack.getCount() < amount
                || !ItemStack.isSameItemSameTags(chargedStack, context.weapon())) {
            return Optional.empty();
        }
        ItemStack expected = chargedStack.copyWithCount(amount);
        boolean[] consumed = {false};
        return Optional.of(PreparedProjectileCost.once(() -> {
            ItemStack current = player.getItemInHand(context.hand());
            if (current != chargedStack
                    || !ItemStack.isSameItemSameTags(current, expected)
                    || current.getCount() < amount) {
                throw new IllegalStateException("Prepared held item changed before commit");
            }
            chargedStack.shrink(amount);
            consumed[0] = true;
        }, () -> {
            if (!consumed[0]) {
                return;
            }
            ItemStack current = player.getItemInHand(context.hand());
            if (current == chargedStack && ItemStack.isSameItemSameTags(chargedStack, expected)) {
                chargedStack.grow(amount);
            } else {
                player.getInventory().placeItemBackInInventory(expected.copy());
            }
            consumed[0] = false;
        }));
    }
}
