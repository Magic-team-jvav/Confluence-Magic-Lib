package org.confluence.lib.common.loot;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import org.confluence.lib.ConfluenceMagicLib;

import java.util.Map;

public record CraftingLootItemCondition(ICondition condition) implements LootItemCondition {
    public static final Serializer<CraftingLootItemCondition> SERIALIZER = new Serializer<>() {
        @Override
        public void serialize(JsonObject json, CraftingLootItemCondition value, JsonSerializationContext serializationContext) {
            JsonObject object = CraftingHelper.serialize(value.condition);
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                json.add(entry.getKey(), entry.getValue());
            }
        }

        @Override
        public CraftingLootItemCondition deserialize(JsonObject json, JsonDeserializationContext serializationContext) {
            return new CraftingLootItemCondition(CraftingHelper.getCondition(json));
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
