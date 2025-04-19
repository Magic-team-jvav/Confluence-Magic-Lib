package org.confluence.lib.common;

import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.confluence.lib.ConfluenceMagicLib;

public final class LibTags {
    public static class Items {
        public static final TagKey<Item> WIP = register("wip");
        public static final TagKey<Item> SKIP_USING_SLOWDOWN = register("skip_using_slowdown");

        private static TagKey<Item> register(String id) {
            return ItemTags.create(ConfluenceMagicLib.lib(id));
        }
    }
}
