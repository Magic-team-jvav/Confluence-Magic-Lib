package org.confluence.lib.common.data.gen;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.DamageTypeTagsProvider;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
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
                DamageTypes.PLAYER_ATTACK
        );
        /// 1.20 `LibDamageTypeTagsProvider:31`（该行在 1.20 是
        /// `tag(DamageTypeTags.IS_PROJECTILE).add(GUN_BULLET, MAGICAL_PROJECTILE)`）。
        /// 1.21 侧此前由 **TerraGuns** 的 `data/minecraft/tags/damage_type/is_projectile.json`
        /// 代填（值却是 `terra_guns:bullet_damage`）——枪械内联 G6′ 退役该模块后必须由 Lib 自己出。
        /// `MAGICAL_PROJECTILE` 在 1.21 的 `LibDamageTypes` 里尚不存在，故此处只列 `GUN_BULLET`；
        /// 补齐 1.20 的 13 个伤害类型（含 `MAGICAL_PROJECTILE`/`SWORD_PROJECTILE`/`SUMMON`）是独立 WP。
        tag(DamageTypeTags.IS_PROJECTILE).add(LibDamageTypes.GUN_BULLET);
    }
}
