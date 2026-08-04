package org.confluence.lib.mixed;

import net.minecraft.world.item.crafting.ShapedRecipePattern;

public interface ILibShapedRecipePattern {
    void confluence$setNonSymmetricalMatching();

    boolean confluence$isNonSymmetricalMatching();

    static void setNonSymmetricalMatching(ShapedRecipePattern pattern) {
        ((ILibShapedRecipePattern) (Object) pattern).confluence$setNonSymmetricalMatching();
    }

    static boolean isNonSymmetricalMatching(ShapedRecipePattern pattern) {
        return ((ILibShapedRecipePattern) (Object) pattern).confluence$isNonSymmetricalMatching();
    }
}
