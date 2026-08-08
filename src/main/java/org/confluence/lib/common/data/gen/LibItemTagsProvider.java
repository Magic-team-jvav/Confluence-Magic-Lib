package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class LibItemTagsProvider extends ItemTagsProvider {
    LibItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, @Nullable ExistingFileHelper helper) {
        super(output, registries, CompletableFuture.completedFuture(tag -> Optional.empty()), ConfluenceMagicLib.LIB_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        tag(LibTags.Items.INGOTS_TIN);
        tag(LibTags.Items.INGOTS_LEAD);
        tag(LibTags.Items.INGOTS_SILVER);
        tag(LibTags.Items.INGOTS_TUNGSTEN);
        tag(LibTags.Items.INGOTS_PLATINUM);
    }
}
