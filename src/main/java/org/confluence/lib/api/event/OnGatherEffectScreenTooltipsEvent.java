package org.confluence.lib.api.event;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

import java.util.function.Consumer;

@Cancelable
public class OnGatherEffectScreenTooltipsEvent extends Event {
    private final MobEffect effect;
    private final ResourceLocation id;
    private final String key;
    private final Consumer<Component> appender;

    public OnGatherEffectScreenTooltipsEvent(MobEffect effect, ResourceLocation id, String key, Consumer<Component> appender) {
        this.effect = effect;
        this.id = id;
        this.key = key;
        this.appender = appender;
    }

    public MobEffect getEffect() {
        return effect;
    }

    public ResourceLocation getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public void append(Component component) {
        appender.accept(component);
    }
}
