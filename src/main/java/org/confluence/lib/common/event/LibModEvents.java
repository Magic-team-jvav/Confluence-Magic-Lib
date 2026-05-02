package org.confluence.lib.common.event;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.api.event.CustomGroupItemIconEvent;
import org.confluence.lib.api.event.NameFixRegisterEvent;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.fluid.FluidBuilder;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.network.AttackDamagePacketS2C;
import org.confluence.lib.network.SetEntityDataPacketS2C;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class LibModEvents {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void register(RegisterEvent event) {
        FluidBuilder.register(event);
        if (Registries.ATTRIBUTE.equals(event.getRegistryKey())) {
            LibAttributes.prepareReplacements();
        }
    }

    @SubscribeEvent
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar("1")
                .playToClient(SetEntityDataPacketS2C.TYPE, SetEntityDataPacketS2C.STREAM_CODEC, SetEntityDataPacketS2C::handle)
                .playToClient(AttackDamagePacketS2C.TYPE, AttackDamagePacketS2C.STREAM_CODEC, AttackDamagePacketS2C::handle)
        ;
    }

    @SubscribeEvent
    public static void fmlLoadComplete(FMLLoadCompleteEvent event) {
        event.enqueueWork(() -> {
            ImmutableMap.Builder<String, String> blockWithItem = ImmutableMap.builder();
            ModLoader.postEvent(new NameFixRegisterEvent.BlockWithItem(blockWithItem));
            Map<String, String> map = blockWithItem.build();

            ImmutableMap.Builder<String, String> block = ImmutableMap.builder();
            ModLoader.postEvent(new NameFixRegisterEvent.Block(block));
            block.putAll(map);
            for (Map.Entry<String, String> entry : block.build().entrySet()) {
                BuiltInRegistries.BLOCK.addAlias(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue()));
            }

            ImmutableMap.Builder<String, String> item = ImmutableMap.builder();
            ModLoader.postEvent(new NameFixRegisterEvent.Item(item));
            item.putAll(map);
            for (Map.Entry<String, String> entry : item.build().entrySet()) {
                BuiltInRegistries.ITEM.addAlias(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue()));
            }
        });
    }

    @SubscribeEvent
    public static void entityAttributeModification(EntityAttributeModificationEvent event) {
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildCreativeModeTabContents(BuildCreativeModeTabContentsEvent event) {
        ResourceKey<CreativeModeTab> tabKey = event.getTabKey();
        if (LibStartupConfig.itemGroups()) {
            CustomGroupItemIconEvent.post();
            if (GroupItem.isInvalidCreativeModeTab(tabKey)) {
                if (tabKey == CreativeModeTabs.SEARCH) { // 移除GroupItem，放入组内搜不到的物品
                    List<ItemStack> groupStacks = event.getParentEntries().stream().filter(stack -> stack.is(GroupItem.getInstance())).toList();
                    for (ItemStack groupStack : groupStacks) {
                        for (ItemStack stack : groupStack.getOrDefault(ConfluenceMagicLib.GROUP_STACKS, GroupItem.Stacks.EMPTY).getValues()) {
                            if (event.getParentEntries().contains(stack)) continue;
                            event.insertBefore(groupStack, stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY); // SearchTab仅使用ParentEntries进行查询
                        }
                        event.remove(groupStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS); // SearchTab不需要Group
                    }
                }
            } else { // 收集belongsTo
                List<Pair<ItemStack, ResourceLocation>> hasBelongsTo = new ArrayList<>();
                Map<ResourceLocation, ItemStack> groupItems = new HashMap<>();
                for (ItemStack stack : event.getParentEntries()) {
                    GroupItem.BelongsTo belongsTo = stack.get(ConfluenceMagicLib.BELONGS_TO_GROUP);
                    if (belongsTo == null) {
                        if (stack.is(GroupItem.getInstance())) {
                            GroupItem.Stacks stacks = stack.get(ConfluenceMagicLib.GROUP_STACKS);
                            if (stacks == null) continue;
                            groupItems.put(stacks.getName(), stack);
                        }
                    } else {
                        hasBelongsTo.add(new Pair<>(stack, belongsTo.name()));
                    }
                }
                for (Pair<ItemStack, ResourceLocation> pair : hasBelongsTo) { // 前面应该已经使用过WipNotDisplayOutput，故在此不检查
                    ItemStack stack = pair.getFirst();
                    ItemStack groupStack = groupItems.computeIfAbsent(pair.getSecond(), rl -> {
                        ItemStack newStack = GroupItem.of(rl);
                        event.insertBefore(stack, newStack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY); // Group不加入SearchTab
                        return newStack;
                    });
                    event.remove(stack, CreativeModeTab.TabVisibility.PARENT_TAB_ONLY); // 保留Search，如果有的话
                    GroupItem.Stacks stacks = groupStack.get(ConfluenceMagicLib.GROUP_STACKS);
                    if (stacks == null) continue;
                    groupStack.set(ConfluenceMagicLib.GROUP_STACKS, stacks.withValues(tabKey, stack));
                }
            }
        }
    }
}
