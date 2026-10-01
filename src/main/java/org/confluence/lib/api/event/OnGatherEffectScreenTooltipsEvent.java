package org.confluence.lib.api.event;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.function.Consumer;

public class OnGatherEffectScreenTooltipsEvent extends Event implements ICancellableEvent {
    private final Holder<MobEffect> effect;
    private final ResourceLocation id;
    private final String key;
    private final Consumer<Component> appender;

    public OnGatherEffectScreenTooltipsEvent(Holder<MobEffect> effect, ResourceLocation id, String key, Consumer<Component> appender) {
        this.effect = effect;
        this.id = id;
        this.key = key;
        this.appender = appender;
    }

    public Holder<MobEffect> getEffect() {
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
