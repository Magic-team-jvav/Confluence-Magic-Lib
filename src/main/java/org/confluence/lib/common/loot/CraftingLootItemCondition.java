package org.confluence.lib.common.loot;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.confluence.lib.ConfluenceMagicLib;

import java.util.stream.Stream;

public record CraftingLootItemCondition(ICondition condition) implements LootItemCondition {
    public static final MapCodec<CraftingLootItemCondition> CODEC = new MapCodec<>() {
        @Override
        public <T> Stream<T> keys(DynamicOps<T> ops) {
            return Stream.empty();
        }

        @Override
        public <T> DataResult<CraftingLootItemCondition> decode(DynamicOps<T> ops, MapLike<T> input) {
            return ICondition.CODEC.parse(ops, ops.createMap(input.entries())).map(CraftingLootItemCondition::new);
        }

        @Override
        public <T> RecordBuilder<T> encode(CraftingLootItemCondition input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
            DataResult<MapLike<T>> encodedMapResult = ICondition.CODEC.encodeStart(ops, input.condition()).flatMap(ops::getMap);
            return encodedMapResult.map(encodedMap -> {
                encodedMap.entries().forEach(pair -> prefix.add(pair.getFirst(), pair.getSecond()));
                return prefix;
            }).result().orElseGet(() -> prefix.withErrorsFrom(encodedMapResult));
        }
    };

    @Override
    public LootItemConditionType getType() {
        return ConfluenceMagicLib.CRAFTING_LOOT_ITEM_CONDITION.get();
    }

    @Override
    public boolean test(LootContext lootContext) {
        return condition.test(lootContext.getLevel().getServer().getServerResources().managers().getConditionContext());
    }
}
