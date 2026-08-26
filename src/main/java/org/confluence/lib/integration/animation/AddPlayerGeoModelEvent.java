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
import software.bernie.geckolib.animation.*;

import java.util.function.Function;

public class AddPlayerGeoModelEvent extends Event implements IModBusEvent {
    private static final MutableHashedLinkedMap<ResourceLocation, Function<Player, @Nullable Group>> groups = new MutableHashedLinkedMap<>();

    @ApiStatus.Internal
    public AddPlayerGeoModelEvent() {
        RawAnimation idle = RawAnimation.begin().thenLoop("greatsword-idle");
        RawAnimation attack = RawAnimation.begin().then("greatsword-attack-1", Animation.LoopType.PLAY_ONCE);
        Group group = new Group(
                ConfluenceMagicLib.asResource("geo/pal.geo.json"),
                ConfluenceMagicLib.asResource("animations/pal.animation.json"),
                state -> {
                    AnimationController<PlayerGeoAnimatable> controller = state.getController();
                    if (state.isAttacking) {
                        if (!attack.equals(controller.getCurrentRawAnimation())) {
                            state.setAnimation(attack);
                            controller.setAnimationSpeedHandler(a -> 20.0 / a.player.getCurrentItemAttackStrengthDelay());
                        }
                        if (controller.hasAnimationFinished()) {
                            state.isAttacking = false;
                        }
                    } else if (!idle.equals(controller.getCurrentRawAnimation())) {
                        state.setAnimation(idle);
                        controller.setAnimationSpeed(1);
                    }
                    return PlayState.CONTINUE;
                }
        );
        add(ConfluenceMagicLib.asResource("test"), player -> player.getMainHandItem().is(Items.DEBUG_STICK) ? group : null);
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
            PlayerAnimationStateHandler handler
    ) {}

    @FunctionalInterface
    public interface PlayerAnimationStateHandler extends AnimationController.AnimationStateHandler<PlayerGeoAnimatable> {
        PlayState handle(PlayerAnimationState state);

        @Override
        default PlayState handle(AnimationState<PlayerGeoAnimatable> state) {
            return handle((PlayerAnimationState) state);
        }
    }
}
