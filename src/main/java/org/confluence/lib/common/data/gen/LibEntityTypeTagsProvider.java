package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class LibEntityTypeTagsProvider extends EntityTypeTagsProvider {
    LibEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup, @Nullable ExistingFileHelper helper) {
        super(output, lookup, ConfluenceMagicLib.LIB_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LibTags.EntityTypes.SLIME).add(EntityType.SLIME);
    }
}
