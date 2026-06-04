package org.confluence.lib.mixin.fixer;

import com.google.common.collect.ImmutableMap;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.BaseMappedRegistry;
import org.confluence.lib.api.event.NameFixRegisterEvent;
import org.mesdag.portlib.event.PortEventHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(RegistryDataLoader.RegistryData.class)
public abstract class RegistryDataLoader$RegistryDataMixin<T> {
    @Shadow
    @Final
    private ResourceKey<? extends Registry<T>> key;

    @ModifyExpressionValue(method = "create", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/registries/RegistryBuilder;create()Lnet/minecraft/core/Registry;"))
    private Registry<T> modifyRegistry(Registry<T> original) {
        if (original instanceof BaseMappedRegistry<T> registry) {
            if (Registries.BIOME.equals(key)) {
                ImmutableMap.Builder<String, String> biome = ImmutableMap.builder();
                PortEventHandler.postEvent(new NameFixRegisterEvent.Biome(biome));
                for (Map.Entry<String, String> entry : biome.build().entrySet()) {
                    registry.addAlias(ResourceLocation.parse(entry.getKey()), ResourceLocation.parse(entry.getValue()));
                }
            }
        }
        return original;
    }
}
