package org.confluence.lib.integration.animation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.util.MutableHashedLinkedMap;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

import java.util.function.Function;

public class AddPlayerGeoModelEvent extends Event implements IModBusEvent {
    private static final MutableHashedLinkedMap<ResourceLocation, Function<Player, @Nullable Group>> groups = new MutableHashedLinkedMap<>();

    @ApiStatus.Internal
    public AddPlayerGeoModelEvent() {
        RawAnimation idle = RawAnimation.begin().thenLoop("greatsword-idle");
        RawAnimation attack = RawAnimation.begin().thenLoop("greatsword-attack-1");
        Group group = new Group(
                ConfluenceMagicLib.asResource("geo/pal.geo.json"),
                ConfluenceMagicLib.asResource("animations/pal.animation.json"),
                state -> {
                    if (state.getAnimatable().player.attackAnim > 0) {
                        return state.setAndContinue(attack);
                    }
                    return state.setAndContinue(idle);
                }
        );
        add(ConfluenceMagicLib.asResource("test"), player -> player.getMainHandItem().is(Items.NETHERITE_SWORD) ? group : null);
    }

    public void add(ResourceLocation id, Function<Player, @Nullable Group> handler) {
        groups.put(id, handler);
    }

    public void addFirst(ResourceLocation id, Function<Player, @Nullable Group> handler) {
        groups.putFirst(id, handler);
    }

    public void addBefore(ResourceLocation before, ResourceLocation id, Function<Player, @Nullable Group> handler) {
        groups.putBefore(before, id, handler);
    }

    public void addAfter(ResourceLocation after, ResourceLocation id, Function<Player, @Nullable Group> handler) {
        groups.putAfter(after, id, handler);
    }

    @ApiStatus.Internal
    public static MutableHashedLinkedMap<ResourceLocation, Function<Player, @Nullable Group>> getGroups() {
        return groups;
    }

    public record Group(
            ResourceLocation model,
            ResourceLocation animation,
            AnimationController.AnimationStateHandler<PlayerGeoAnimatable> handler
    ) {}
}
