package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.common.component.ModRarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
    @Shadow
    public abstract ItemStack getItem();

    @WrapOperation(method = "hurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/Item;canBeHurtBy(Lnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean fireResistant(Item instance, DamageSource damageSource, Operation<Boolean> original) {
        boolean canBeHurt = original.call(instance, damageSource);
        if (canBeHurt && damageSource.is(DamageTypeTags.IS_FIRE)) {
            ModRarity rarity = ModRarity.getRarity(getItem());
            return rarity != null && rarity != ModRarity.WHITE && rarity != ModRarity.GRAY;
        }
        return canBeHurt;
    }
}
