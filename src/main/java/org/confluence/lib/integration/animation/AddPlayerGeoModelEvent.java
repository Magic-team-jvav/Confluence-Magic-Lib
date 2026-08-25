package org.confluence.lib.integration.animation;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.Event;
import net.neoforged.fml.event.IModBusEvent;
import net.neoforged.neoforge.common.util.MutableHashedLinkedMap;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;

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
                    PlayerGeoAnimatable animatable = state.getAnimatable();
                    AnimationController<PlayerGeoAnimatable> controller = state.getController();
                    if (animatable.player.getAttackStrengthScale(state.getPartialTick()) < 1) {
                        controller.setAnimationSpeedHandler(a -> 20.0 / a.player.getCurrentItemAttackStrengthDelay());
                        return state.setAndContinue(attack);
                    }
                    controller.setAnimationSpeed(1);
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
