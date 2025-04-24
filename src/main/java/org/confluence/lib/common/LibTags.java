package org.confluence.lib.common;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.confluence.lib.ConfluenceMagicLib;

public final class LibTags {
    public static class Items {
        public static final TagKey<Item> WIP = register("wip");
        public static final TagKey<Item> SKIP_USING_SLOWDOWN = register("skip_using_slowdown"); // 使用时不影响玩家移动速度
        public static final TagKey<Item> SKIP_RESET_STRENGTH = register("skip_reset_strength"); // 使用时不重置玩家攻击冷却

        private static TagKey<Item> register(String id) {
            return ItemTags.create(ConfluenceMagicLib.lib(id));
        }
    }
}
