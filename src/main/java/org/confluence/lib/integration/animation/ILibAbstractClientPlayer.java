package org.confluence.lib.integration.animation;

import net.minecraft.client.player.AbstractClientPlayer;

public interface ILibAbstractClientPlayer {
    PlayerGeoAnimatable confluence$getAnimatable();

    static ILibAbstractClientPlayer of(AbstractClientPlayer player) {
        return (ILibAbstractClientPlayer) player;
    }
}
