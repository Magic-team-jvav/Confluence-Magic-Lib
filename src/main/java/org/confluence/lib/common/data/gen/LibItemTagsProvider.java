package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.ItemTagsProvider;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public class LibItemTagsProvider extends ItemTagsProvider {
    public LibItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ConfluenceMagicLib.LIB_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(LibTags.Items.INGOTS_PLATINUM);
        tag(LibTags.Items.INGOTS_SILVER);
        tag(LibTags.Items.INGOTS_TIN);
        tag(LibTags.Items.INGOTS_TUNGSTEN);
    }
}
