package org.confluence.lib.mixin;

import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.confluence.lib.mixed.ILibShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = ShapedRecipePattern.class, priority = 0)
public abstract class ShapedRecipePatternMixin implements ILibShapedRecipePattern {
    @Unique
    private boolean confluence$nonSymmetricalMatching = false;

    @Override
    public void confluence$setNonSymmetricalMatching() {
        this.confluence$nonSymmetricalMatching = true;
    }

    @Override
    public boolean confluence$isNonSymmetricalMatching() {
        return confluence$nonSymmetricalMatching;
    }

    @ModifyVariable(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Z)Z", at = @At("HEAD"), argsOnly = true, name = "xFlip")
    private boolean nonSymmetrical(boolean xFlip) {
        if (xFlip && confluence$nonSymmetricalMatching) return false;
        return xFlip;
    }
}
