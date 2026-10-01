package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.LibDamageTypes;
import org.confluence.lib.common.LibTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public final class LibDamageTypeTagsProvider extends DamageTypeTagsProvider {
    LibDamageTypeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, @Nullable ExistingFileHelper helper) {
        super(output, registries, ConfluenceMagicLib.LIB_ID, helper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        tag(LibTags.DamageTypes.AS_MELEE_ATTACK).add(
                DamageTypes.MOB_ATTACK,
                DamageTypes.MOB_ATTACK_NO_AGGRO,
                DamageTypes.PLAYER_ATTACK,
                DamageTypes.STING,
                LibDamageTypes.SWORD_PROJECTILE);
        tag(Tags.DamageTypes.IS_MAGIC).add(LibDamageTypes.MAGICAL_PROJECTILE);
        tag(DamageTypeTags.IS_PROJECTILE).add(LibDamageTypes.GUN_BULLET, LibDamageTypes.MAGICAL_PROJECTILE);
        tag(DamageTypeTags.BYPASSES_ARMOR).add(LibDamageTypes.DUNGEON_GUARDIAN);
        tag(DamageTypeTags.IS_PLAYER_ATTACK).add(LibDamageTypes.SUMMON);
    }
}
