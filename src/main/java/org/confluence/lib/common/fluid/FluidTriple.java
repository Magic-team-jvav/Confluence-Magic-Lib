package org.confluence.lib.common.fluid;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidType;
import org.mesdag.portlib.registries.PortRegistryEntry;

public record FluidTriple(
        PortRegistryEntry<FluidType, FluidType> type,
        PortRegistryEntry<Fluid, FlowingFluid> fluid,
        PortRegistryEntry<Fluid, FlowingFluid> flowing
) {
    public static FluidBuilder builder(ResourceLocation location) {
        FluidBuilder builder = new FluidBuilder(location);
        FluidBuilder.BUILDERS.put(location, builder);
        return builder;
    }
}
