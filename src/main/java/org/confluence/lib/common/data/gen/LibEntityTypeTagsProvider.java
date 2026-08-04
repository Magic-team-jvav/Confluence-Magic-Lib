package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.EntityTypeTagsProvider;
import net.minecraft.world.entity.EntityType;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.CompletableFuture;

@ApiStatus.Internal
public class LibEntityTypeTagsProvider extends EntityTypeTagsProvider {
    public LibEntityTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookup) {
        super(output, lookup, ConfluenceMagicLib.LIB_ID);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LibTags.EntityTypes.SLIME).add(EntityType.SLIME);
    }
}
