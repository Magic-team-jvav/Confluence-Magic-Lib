package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibDamageTypes;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = ConfluenceMagicLib.LIB_ID)
public final class LibDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper helper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();

        boolean server = event.includeServer();
        RegistrySetBuilder builder = new RegistrySetBuilder().add(Registries.DAMAGE_TYPE, LibDamageTypes::bootstrap);
        DatapackBuiltinEntriesProvider registryProvider = new DatapackBuiltinEntriesProvider(output, lookup, builder, Set.of(ConfluenceMagicLib.LIB_ID));
        lookup = generator.addProvider(server, registryProvider).getRegistryProvider();

        boolean client = event.includeClient();
        generator.addProvider(client, new LibLanguageProvider(output, true));
        generator.addProvider(client, new LibLanguageProvider(output, false));

        generator.addProvider(server, new LibEntityTypeTagsProvider(output, lookup, helper));
        generator.addProvider(server, new LibDamageTypeTagsProvider(output, lookup, helper));
        generator.addProvider(server, new LibItemTagsProvider(output, lookup, helper));
    }
}
