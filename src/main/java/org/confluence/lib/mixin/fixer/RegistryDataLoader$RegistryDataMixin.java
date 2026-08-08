package org.confluence.lib.mixin.fixer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModLoader;
import net.neoforged.neoforge.registries.BaseMappedRegistry;
import org.confluence.lib.api.event.NameFixRegisterEvent;
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

    @SuppressWarnings("UnstableApiUsage")
    @ModifyExpressionValue(method = "create", at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/registries/RegistryBuilder;create()Lnet/minecraft/core/Registry;"))
    private Registry<T> modifyRegistry(Registry<T> original) {
        if (original instanceof BaseMappedRegistry<T> registry) {
            Map<ResourceLocation, ResourceLocation> alias = ModLoader.postEventWithReturn(new NameFixRegisterEvent.Data(key)).getAlias();
            for (Map.Entry<ResourceLocation, ResourceLocation> entry : alias.entrySet()) {
                registry.addAlias(entry.getKey(), entry.getValue());
            }
        }
        return original;
    }
}
