package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibDamageTypes;

import java.util.concurrent.CompletableFuture;

@EventBusSubscriber(modid = ConfluenceMagicLib.LIB_ID)
public final class LibDataGenerator {
    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        event.createDatapackRegistryObjects(new RegistrySetBuilder()
                .add(Registries.DAMAGE_TYPE, LibDamageTypes::bootstrap));

        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        CompletableFuture<HolderLookup.Provider> lookup = event.getLookupProvider();
        ExistingFileHelper helper = event.getExistingFileHelper();

        boolean client = event.includeClient();
        generator.addProvider(client, new LibLanguageProvider(output, true));
        generator.addProvider(client, new LibLanguageProvider(output, false));

        boolean server = event.includeServer();
        generator.addProvider(server, new LibEntityTypeTagsProvider(output, lookup, helper));
        generator.addProvider(server, new LibDamageTypeTagsProvider(output, lookup, helper));
        generator.addProvider(server, new LibItemTagsProvider(output, lookup, helper));
    }
}
