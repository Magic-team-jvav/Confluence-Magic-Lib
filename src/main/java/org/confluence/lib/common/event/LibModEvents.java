package org.confluence.lib.common.event;

import PortLib.extensions.net.minecraft.world.item.ItemStack.PortItemStackExtension;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.event.CustomGroupItemIconEvent;
import org.confluence.lib.api.event.NameFixRegisterEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.fluid.FluidBuilder;
import org.confluence.lib.common.item.GroupItem;
import org.mesdag.portlib.event.PortEventHandler;
import org.mesdag.portlib.event.PortEventPriority;
import org.mesdag.portlib.event.entity.PortEntityAttributeModificationEvent;
import org.mesdag.portlib.event.other.PortBuildCreativeModeTabContentsEvent;
import org.mesdag.portlib.event.registries.PortRegisterEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LibModEvents {
    public static void init() {
        PortEventHandler.addListener(PortEventPriority.LOWEST, LibModEvents::register);
        PortEventHandler.addListener(LibModEvents::entityAttributeModification);
        PortEventHandler.addListener(PortEventPriority.LOWEST, LibModEvents::buildCreativeModeTabContents);
    }

    private static Map<String, String> blockWithItemMap;

    private static void register(PortRegisterEvent event) {
        FluidBuilder.register(event);
        ResourceKey<? extends Registry<?>> registryKey = event.getRegistryKey();
        if (Registries.ATTRIBUTE.equals(registryKey)) {
            LibAttributes.prepareReplacements();
        } else if (Registries.BLOCK.equals(registryKey)) {
            ImmutableMap.Builder<String, String> blockWithItem = ImmutableMap.builder();
            PortEventHandler.postEvent(new NameFixRegisterEvent.BlockWithItem(blockWithItem));
            blockWithItemMap = blockWithItem.build();
            ImmutableMap.Builder<String, String> block = ImmutableMap.builder();
            PortEventHandler.postEvent(new NameFixRegisterEvent.Block(block));
            block.putAll(blockWithItemMap);
            for (Map.Entry<String, String> entry : block.build().entrySet()) {
                ((ForgeRegistry<Block>) ForgeRegistries.BLOCKS).addAlias(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue()));
            }
        } else if (Registries.ITEM.equals(registryKey)) {
            ImmutableMap.Builder<String, String> item = ImmutableMap.builder();
            PortEventHandler.postEvent(new NameFixRegisterEvent.Item(item));
            item.putAll(blockWithItemMap);
            for (Map.Entry<String, String> entry : item.build().entrySet()) {
                ((ForgeRegistry<Item>) ForgeRegistries.ITEMS).addAlias(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue()));
            }
        }
    }

    private static void entityAttributeModification(PortEntityAttributeModificationEvent event) {
        LibAttributes.registerAttribute(ConfluenceMagicLib.CRITICAL_CHANCE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.RANGED_VELOCITY, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.RANGED_DAMAGE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.DODGE_CHANCE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.AGGRO, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.MAGIC_DAMAGE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.ARMOR_PENETRATION, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.PICKUP_RANGE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.MINION_CAPACITY, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.SENTRY_CAPACITY, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.SUMMON_DAMAGE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.SUMMON_KNOCKBACK, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.WHIP_RANGE, event::add);
        LibAttributes.registerAttribute(ConfluenceMagicLib.MARK_DAMAGE, event::add);
    }

    private static void buildCreativeModeTabContents(PortBuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tabKey = event.getTabKey();
        if (LibStartupConfig.itemGroups()) {
            CustomGroupItemIconEvent.post();
            if (GroupItem.isInvalidCreativeModeTab(tabKey)) {
                if (tabKey == CreativeModeTabs.SEARCH) {
                    List<ItemStack> groupStacks = Streams.stream(event.getEntries())
                            .filter(entry -> PortBuildCreativeModeTabContentsEvent.isParentTab(entry.getValue()))
                            .map(Map.Entry::getKey)
                            .filter(stack -> stack.is(GroupItem.getInstance())).toList();
                    for (ItemStack groupStack : groupStacks) {
                        for (ItemStack stack : PortItemStackExtension.getDataOrDefault(groupStack, ConfluenceMagicLib.GROUP_STACKS, GroupItem.Stacks.EMPTY).getValues()) {
                            if (event.getEntries().contains(stack)) continue;
                            event.insertBefore(groupStack, stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        }
                        event.remove(groupStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                    }
                }
            } else {
                List<Pair<ItemStack, ResourceLocation>> hasBelongsTo = new ArrayList<>();
                Map<ResourceLocation, ItemStack> groupItems = new HashMap<>();
                for (Map.Entry<ItemStack, CreativeModeTab.TabVisibility> entry : event.getEntries()) {
                    if (PortBuildCreativeModeTabContentsEvent.isParentTab(entry.getValue())) {
                        ItemStack stack = entry.getKey();
                        GroupItem.BelongsTo belongsTo = PortItemStackExtension.getData(stack, ConfluenceMagicLib.BELONGS_TO_GROUP);
                        if (belongsTo == null) {
                            if (stack.is(GroupItem.getInstance())) {
                                GroupItem.Stacks stacks = PortItemStackExtension.getData(stack, ConfluenceMagicLib.GROUP_STACKS);
                                if (stacks == null) continue;
                                groupItems.put(stacks.getName(), stack);
                            }
                        } else {
                            hasBelongsTo.add(new Pair<>(stack, belongsTo.name()));
                        }
                    }
                }
                for (Pair<ItemStack, ResourceLocation> pair : hasBelongsTo) {
                    ItemStack stack = pair.getFirst();
                    ItemStack groupStack = groupItems.computeIfAbsent(pair.getSecond(), rl -> {
                        ItemStack newStack = GroupItem.of(rl);
                        event.insertBefore(stack, newStack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                        return newStack;
                    });
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY);
                    GroupItem.Stacks stacks = PortItemStackExtension.getData(groupStack, ConfluenceMagicLib.GROUP_STACKS);
                    if (stacks == null) continue;
                    PortItemStackExtension.setData(groupStack, ConfluenceMagicLib.GROUP_STACKS, stacks.withValues(tabKey, stack));
                }
            }
        }
    }
}
