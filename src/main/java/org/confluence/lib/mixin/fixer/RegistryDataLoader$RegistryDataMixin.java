package org.confluence.lib.mixin.fixer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryDataLoader;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.confluence.lib.api.event.NameFixRegisterEvent;
import org.mesdag.portlib.diff.IPortMappedRegistry;
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

    @ModifyExpressionValue(method = "create", at = @At(value = "NEW", target = "(Lnet/minecraft/resources/ResourceKey;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/core/MappedRegistry;"))
    private MappedRegistry<T> modifyRegistry(MappedRegistry<T> original) {
        IPortMappedRegistry<T> registry = IPortMappedRegistry.of(original);
        Map<ResourceLocation, ResourceLocation> alias = PortEventHandler.postEventWithReturn(new NameFixRegisterEvent.Data(key)).getAlias();
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : alias.entrySet()) {
            registry.portlib$addAlias(entry.getKey(), entry.getValue());
        }
        return original;
    }
}
