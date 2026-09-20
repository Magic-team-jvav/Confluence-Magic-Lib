package org.confluence.lib.api.animation.third_person;

import net.minecraft.client.player.AbstractClientPlayer;

public interface ILibAbstractClientPlayer {
    PlayerGeoAnimatable confluence$getAnimatable();

    static ILibAbstractClientPlayer of(AbstractClientPlayer player) {
        return (ILibAbstractClientPlayer) player;
    }
}
