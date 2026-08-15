package org.confluence.lib.api.permanent;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 附属模组可以直接注册的通用永久强化物品。
 *
 * <p>客户端只播放使用预测；资格检查、等级写入、物品消耗和进度触发都由服务端完成。</p>
 */
public class PermanentUpgradeItem extends Item {
    private final PermanentUpgrade upgrade;
    private final int levelDelta;
    private final @Nullable Supplier<SoundEvent> sound;
    private final List<Component> tooltips;

    /**
     * 创建每次使用增加一级的永久强化物品。
     */
    public PermanentUpgradeItem(Properties properties, PermanentUpgrade upgrade,
                                @Nullable Supplier<SoundEvent> sound, List<Component> tooltips) {
        this(properties, upgrade, 1, sound, tooltips);
    }

    /**
     * 使用负增量可以创建回溯类物品。
     */
    public PermanentUpgradeItem(Properties properties, PermanentUpgrade upgrade, int levelDelta,
                                @Nullable Supplier<SoundEvent> sound, List<Component> tooltips) {
        super(properties);
        if (levelDelta == 0) throw new IllegalArgumentException("levelDelta must not be zero");
        this.upgrade = Objects.requireNonNull(upgrade, "upgrade");
        this.levelDelta = levelDelta;
        this.sound = sound;
        this.tooltips = List.copyOf(Objects.requireNonNull(tooltips, "tooltips"));
    }

    /**
     * 返回该物品使用的公开强化定义。
     */
    public PermanentUpgrade upgrade() {
        return upgrade;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.consume(stack);
        if (!(player instanceof ServerPlayer serverPlayer))
            return InteractionResultHolder.fail(stack);

        PermanentUpgradeResult result = upgrade.tryChange(serverPlayer, levelDelta);
        if (!result.isApplied()) {
            if (result.translationKey() != null) {
                serverPlayer.displayClientMessage(Component.translatable(result.translationKey()), true);
            }
            return InteractionResultHolder.fail(stack);
        }

        ItemStack consumedStack = stack.copy();
        consumedStack.setCount(1);
        if (!player.hasInfiniteMaterials()) stack.shrink(1);
        if (sound != null) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), sound.get(),
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, consumedStack);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents,
                                TooltipFlag isAdvanced) {
        tooltipComponents.addAll(tooltips);
    }
}
