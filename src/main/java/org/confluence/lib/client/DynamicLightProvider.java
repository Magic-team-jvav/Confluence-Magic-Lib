package org.confluence.lib.client;

@FunctionalInterface
public interface DynamicLightProvider<T> {

    DynamicLightDispatcher.LightSource getLuminance(T instance, float partialTick);

}
