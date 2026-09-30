package org.confluence.lib.client.event;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.LibStartupConfig;
import org.confluence.lib.client.LibKeyBindings;
import org.confluence.lib.client.particle.CrossDustParticle;
import org.confluence.lib.client.render.item.GroupItemExtension;
import org.confluence.lib.common.item.GroupItem;
import org.confluence.lib.api.animation.third_person.AddPlayerGeoModelEvent;
import org.confluence.lib.api.animation.third_person.AnimationConstants;
import org.confluence.lib.api.animation.third_person.PlayerGeoAnimatable;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)
public final class LibClientModEvents {
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CrossDustParticle.Provider::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        if (LibStartupConfig.itemGroups()) {
            event.registerItem(GroupItemExtension.INSTANCE, GroupItem.getInstance());
        }
    }

    @SubscribeEvent
    public static void fmlClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (AnimationConstants.SHOULD_APPLY) {
                ModLoader.postEvent(new AddPlayerGeoModelEvent());
            }
        });
    }

    @SubscribeEvent
    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        if (AnimationConstants.SHOULD_APPLY) {
            event.registerReloadListener((pb, rm, pp, rp, be, ge) -> CompletableFuture.runAsync(() -> {
                for (Runnable callback : PlayerGeoAnimatable.reloadCallbacks) {
                    callback.run();
                }
            }, ge).thenCompose(pb::wait));
        }
    }

    /// 重力反转按键的注册（**WP6c 第二步：Lib 侧接线**）。
    ///
    /// 1.20 是 `LibKeyBindings.init()` 里的 `PortEventHandler.addListener(LibKeyBindings::registerKeyMapping)`
    /// （PortLib 按事件类型自动路由到模组总线）；1.21 的等价入口就是本类 —— 它已经是
    /// **客户端模组总线**订阅者：`@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID, value = Dist.CLIENT)`
    /// （本类上面的 `RegisterParticleProvidersEvent` / `RegisterClientExtensionsEvent` /
    /// `RegisterClientReloadListenersEvent` 都是 `IModBusEvent`，说明 NeoForge 21.1 会按
    /// `IModBusEvent` 自动选总线，不需要写 `bus = Bus.MOD`）。
    ///
    /// `RegisterKeyMappingsEvent` 同样 implements `IModBusEvent`
    /// （源码已核实：`net/neoforged/neoforge/client/event/RegisterKeyMappingsEvent.java`
    /// 的 `extends Event implements IModBusEvent`，javadoc 写明 "fired on the mod-specific event bus"），
    /// 所以**只能**挂在模组总线的订阅者上；写成 `NeoForge.EVENT_BUS` 会**静默不触发**。
    ///
    /// ⚠️ 因此 `LibKeyBindings#init(IEventBus)`（1.20 形态的入口，由另一个 agent 落地）
    /// **保持未被调用**：注解式订阅的类拿不到 `IEventBus`，也不需要。两者只能留一处，
    /// 这里选了与本类其它三个注册处理器一致的那种（也是任务书指定的「Lib 现有的客户端模组总线入口」）。
    @SubscribeEvent
    public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(LibKeyBindings.FLIP_GRAVITATION.get());
    }
}
