package org.confluence.lib.api.event;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.event.IModBusEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class CustomGroupItemIconEvent extends Event implements IModBusEvent {
    private static boolean registered;
    private static final Map<ResourceKey<CreativeModeTab>, Map<Identifier, ItemStack>> icons = new Reference2ObjectOpenHashMap<>();

    private CustomGroupItemIconEvent() {}

    public void register(ResourceKey<CreativeModeTab> tabKey, Identifier groupName, ItemStack iconStack) {
        register(tabKey, groupName, iconStack, getMap(tabKey));
    }

    public void register(ResourceKey<CreativeModeTab> tabKey, Consumer<CustomIconHelper> consumer) {
        Map<Identifier, ItemStack> map = getMap(tabKey);
        consumer.accept((groupName, iconStack) -> register(tabKey, groupName, iconStack, map));
    }

    private static Map<Identifier, ItemStack> getMap(ResourceKey<CreativeModeTab> tabKey) {
        return icons.computeIfAbsent(tabKey, key -> new Object2ObjectOpenHashMap<>());
    }

    private static void register(ResourceKey<CreativeModeTab> tabKey, Identifier groupName, ItemStack iconStack, Map<Identifier, ItemStack> map) {
        if (map.put(groupName, iconStack) != null) {
            throw new IllegalArgumentException("Duplicate icon registered! tabKey: " + tabKey + ", groupName: " + groupName + ", iconStack: " + iconStack);
        }
    }

    public static void post() {
        if (registered) return;
        registered = true;
        ModLoader.postEvent(new CustomGroupItemIconEvent());
    }

    public static @Nullable ItemStack getIcon(ResourceKey<CreativeModeTab> tabKey, Identifier groupName) {
        Map<Identifier, ItemStack> map = icons.get(tabKey);
        return map == null ? null : map.get(groupName);
    }

    @FunctionalInterface
    public interface CustomIconHelper {
        void register(Identifier groupName, ItemStack iconStack);
    }
}
