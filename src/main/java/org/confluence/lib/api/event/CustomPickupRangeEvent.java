package org.confluence.lib.api.event;

import it.unimi.dsi.fastutil.objects.ObjectDoubleImmutablePair;
import it.unimi.dsi.fastutil.objects.ObjectDoublePair;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class CustomPickupRangeEvent extends PlayerEvent {
    private Reference2ObjectMap<RangeType, ObjectDoublePair<Predicate<ItemStack>>> ranges;

    public CustomPickupRangeEvent(Player player) {
        super(player);
    }

    public void addRange(RangeType type, double range, Predicate<ItemStack> filter) {
        if (range <= 0) return;
        if (ranges == null) {
            ranges = new Reference2ObjectOpenHashMap<>();
            ranges.put(RangeType.DEFAULT, new ObjectDoubleImmutablePair<>(stack -> true, getEntity().getAttributeValue(ConfluenceMagicLib.PICKUP_RANGE)));
        }
        ranges.put(type, new ObjectDoubleImmutablePair<>(filter, range));
    }

    public double getRange(RangeType type) {
        if (ranges == null) return -1;
        ObjectDoublePair<Predicate<ItemStack>> pair = ranges.get(type);
        if (pair == null) return -1;
        return pair.rightDouble();
    }

    @ApiStatus.Internal
    public @Nullable Reference2ObjectMap<RangeType, ObjectDoublePair<Predicate<ItemStack>>> getRanges() {
        return ranges;
    }

    public static class RangeType {
        private static final Map<ResourceLocation, RangeType> TYPES = new ConcurrentHashMap<>();
        public static final RangeType DEFAULT = get(ConfluenceMagicLib.asResource("default"));

        private final ResourceLocation id;

        private RangeType(ResourceLocation id) {
            this.id = id;
        }

        public ResourceLocation id() {
            return id;
        }

        public static RangeType get(ResourceLocation id) {
            return TYPES.computeIfAbsent(id, RangeType::new);
        }
    }
}
