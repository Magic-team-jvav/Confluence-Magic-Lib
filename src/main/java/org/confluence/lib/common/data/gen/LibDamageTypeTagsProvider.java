package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibDamageTypes;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.Nullable;
import org.mesdag.portlib.wrapper.common.PortTags;

import java.util.concurrent.CompletableFuture;

public final class LibDamageTypeTagsProvider extends DamageTypeTagsProvider {
    LibDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ConfluenceMagicLib.LIB_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LibTags.DamageTypes.AS_MELEE_ATTACK).add(
                DamageTypes.MOB_ATTACK,
                DamageTypes.MOB_ATTACK_NO_AGGRO,
                DamageTypes.PLAYER_ATTACK,
                DamageTypes.STING,
                LibDamageTypes.SWORD_PROJECTILE);
        tag(PortTags.DamageTypes.IS_MAGIC).add(LibDamageTypes.MAGICAL_PROJECTILE);
        tag(DamageTypeTags.IS_PROJECTILE).add(LibDamageTypes.GUN_BULLET, LibDamageTypes.MAGICAL_PROJECTILE);
        tag(DamageTypeTags.BYPASSES_ARMOR).add(LibDamageTypes.DUNGEON_GUARDIAN);
        tag(PortTags.DamageTypes.IS_PLAYER_ATTACK).add(LibDamageTypes.SUMMON);
    }
}
