package org.confluence.lib.mixin;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import org.confluence.lib.mixed.IShapedRecipePattern;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShapedRecipePattern.class, priority = 0)
public abstract class ShapedRecipePatternMixin implements IShapedRecipePattern {
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

    @ModifyVariable(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Z)Z", at = @At("HEAD"), argsOnly = true)
    private boolean nonSymmetrical(boolean symmetrical) {
        if (symmetrical && confluence$nonSymmetricalMatching) return false;
        return symmetrical;
    }

    @Inject(method = "toNetwork", at = @At("TAIL"))
    private void encode(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        buffer.writeBoolean(confluence$nonSymmetricalMatching);
    }

    @Inject(method = "fromNetwork", at = @At("RETURN"))
    private static void decode(RegistryFriendlyByteBuf buffer, CallbackInfoReturnable<ShapedRecipePattern> cir) {
        if (buffer.readBoolean()) {
            IShapedRecipePattern.setNonSymmetricalMatching(cir.getReturnValue());
        }
    }
}
