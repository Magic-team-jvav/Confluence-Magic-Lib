package org.confluence.lib.common.fluid;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.mesdag.portlib.PortLib;
import org.mesdag.portlib.event.registries.PortRegisterEvent;
import org.mesdag.portlib.registries.PortRegistryEntry;

import java.util.Hashtable;
import java.util.function.Consumer;

public class FluidBuilder {
    static final Hashtable<ResourceLocation, FluidBuilder> BUILDERS = new Hashtable<>();
    private final PortRegistryEntry<FluidType, FluidType> type;
    private final PortRegistryEntry<Fluid, FlowingFluid> fluid;
    private final PortRegistryEntry<Fluid, FlowingFluid> flowing;
    private Consumer<FluidType.Properties> typePropertiesConsumer = properties -> {};
    private Consumer<ForgeFlowingFluid.Properties> basePropertiesConsumer = properties -> {};

    FluidBuilder(ResourceLocation location) {
        this.type = PortRegistryEntry.wrap(location, RegistryObject.create(location, ForgeRegistries.Keys.FLUID_TYPES, PortLib.MODID));
        this.fluid = PortRegistryEntry.wrap(location, RegistryObject.create(location, ForgeRegistries.FLUIDS));
        ResourceLocation flowing_ = location.withPrefix("flowing_");
        this.flowing = PortRegistryEntry.wrap(flowing_, RegistryObject.create(flowing_, ForgeRegistries.FLUIDS));
    }

    public FluidBuilder typeProperties(Consumer<FluidType.Properties> consumer) {
        this.typePropertiesConsumer = consumer;
        return this;
    }

    public FluidBuilder baseProperties(Consumer<ForgeFlowingFluid.Properties> consumer) {
        this.basePropertiesConsumer = consumer;
        return this;
    }

    public FluidTriple build() {
        return new FluidTriple(type, fluid, flowing);
    }

    public static void register(PortRegisterEvent event) {
        BUILDERS.forEach((location, builder) -> {
            event.register(ForgeRegistries.Keys.FLUID_TYPES, helper -> {
                FluidType.Properties properties = FluidType.Properties.create();
                builder.typePropertiesConsumer.accept(properties);
                helper.register(builder.type.getId(), new FluidType(properties));
            });

            event.register(Registries.FLUID, helper -> {
                ForgeFlowingFluid.Properties properties = new ForgeFlowingFluid.Properties(builder.type, builder.fluid, builder.flowing);
                builder.basePropertiesConsumer.accept(properties);
                helper.register(builder.fluid.getId(), new ForgeFlowingFluid.Source(properties));
                helper.register(builder.flowing.getId(), new ForgeFlowingFluid.Flowing(properties));
            });
        });
    }
}
