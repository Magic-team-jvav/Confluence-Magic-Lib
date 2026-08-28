package org.confluence.lib.mixin.integration.animation;

import net.minecraft.client.player.AbstractClientPlayer;
import org.confluence.lib.integration.animation.ILibAbstractClientPlayer;
import org.confluence.lib.integration.animation.PlayerGeoAnimatable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin implements ILibAbstractClientPlayer {
    @Unique
    private final PlayerGeoAnimatable confluence$animatable = PlayerGeoAnimatable.choose((AbstractClientPlayer) (Object) this);

    @Override
    public PlayerGeoAnimatable confluence$getAnimatable() {
        return confluence$animatable;
    }
}
