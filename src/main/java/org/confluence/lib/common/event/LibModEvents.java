package org.confluence.lib.common.event;

import com.google.common.collect.ImmutableMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibAttributes;
import org.confluence.lib.common.fluid.FluidBuilder;
import org.confluence.lib.event.NameFixRegisterEvent;
import org.confluence.lib.network.AttackDamagePacketS2C;
import org.confluence.lib.network.SetEntityDataPacketS2C;

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
}
