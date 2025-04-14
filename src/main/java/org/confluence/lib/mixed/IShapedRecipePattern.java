package org.confluence.lib.mixed;

import net.minecraft.world.item.crafting.ShapedRecipePattern;

public interface IShapedRecipePattern {
    void confluence$setNonSymmetricalMatching();

    boolean confluence$isNonSymmetricalMatching();

    static void setNonSymmetricalMatching(ShapedRecipePattern pattern) {
        ((IShapedRecipePattern) (Object) pattern).confluence$setNonSymmetricalMatching();
    }

    static boolean isNonSymmetricalMatching(ShapedRecipePattern pattern) {
        return ((IShapedRecipePattern) (Object) pattern).confluence$isNonSymmetricalMatching();
    }
}
