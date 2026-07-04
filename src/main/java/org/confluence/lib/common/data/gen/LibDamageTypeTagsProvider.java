package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class LibDamageTypeTagsProvider extends DamageTypeTagsProvider {
    public LibDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ConfluenceMagicLib.LIB_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LibTags.DamageTypes.AS_MELEE_ATTACK).add(
                DamageTypes.MOB_ATTACK,
                DamageTypes.PLAYER_ATTACK
        );
    }
}
