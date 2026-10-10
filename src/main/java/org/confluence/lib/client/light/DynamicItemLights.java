package org.confluence.lib.client.light;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;

public final class DynamicItemLights {
    private static final Map<Item, Integer> items = new HashMap<>();
    private static final Map<Item, Integer> projectiles = new HashMap<>();
    private static final List<ItemRule> overrides = new ArrayList<>();
    private static final List<ToIntFunction<ItemStack>> fallbacks = new ArrayList<>();

    private DynamicItemLights() {}

    public static void register(Item item, int brightness) {items.merge(item, brightness, Math::max);}

    public static void registerProjectile(Item item, int brightness) {projectiles.merge(item, brightness, Math::max);}

    // 特殊物品的开关规则优先于固定亮度和兜底规则，返回零即可关闭该物品的光源。
    public static void registerOverride(Class<? extends Item> type, Provider provider) {
        overrides.add(new ItemRule(type, provider));
    }

    public static void registerFallback(ToIntFunction<ItemStack> provider) {fallbacks.add(provider);}

    public static int luminance(ItemStack stack, @Nullable LivingEntity holder, boolean projectile) {
        if (stack.isEmpty()) return 0;
        for (ItemRule rule : overrides) {
            if (rule.type().isInstance(stack.getItem()))
                return rule.provider().luminance(stack, holder, projectile);
        }
        int light = (projectile ? projectiles : items).getOrDefault(stack.getItem(), 0);
        if (stack.getItem() instanceof BlockItem block)
            light = Math.max(light, blockLight(stack, block));
        for (ToIntFunction<ItemStack> provider : fallbacks)
            light = Math.max(light, provider.applyAsInt(stack));
        return light;
    }

    @FunctionalInterface
    public interface Provider {
        int luminance(ItemStack stack, @Nullable LivingEntity holder, boolean projectile);
    }

    private record ItemRule(Class<? extends Item> type, Provider provider) {}

    private static int blockLight(ItemStack stack, BlockItem block) {
        var state = block.getBlock().defaultBlockState();
        var properties = stack.get(DataComponents.BLOCK_STATE);
        if (properties != null) state = properties.apply(state);
        return state.getLightEmission();
    }

}
