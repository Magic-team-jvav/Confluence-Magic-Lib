package org.confluence.lib.api.animation.first_person;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;

public final class HandAnimationApi {
    private HandAnimationApi() {}

    public static boolean play(GeoItem animatable, ItemStack itemStack, ServerPlayer serverPlayer, HandAnimationProfile profile, HandAnimationAction action) {
        boolean played = false;
        for (HandAnimationChannel channel : profile.channels()) {
            if (channel.clip(action).isEmpty()) {
                continue;
            }
            stopAndPlayAnim(animatable, itemStack, serverPlayer, channel.name(), action.id());
            played = true;
        }
        return played;
    }

    /// Stop a triggered action on every channel that declares it. Actions are
    /// channel-local in GeckoLib, so stopping one controller is not enough for
    /// profiles that split hand/camera and weapon animations.
    public static boolean stop(GeoItem animatable, ItemStack itemStack, ServerPlayer serverPlayer, HandAnimationProfile profile, HandAnimationAction action) {
        long instanceId = GeoItem.getOrAssignId(itemStack, serverPlayer.serverLevel());
        boolean stopped = false;
        for (HandAnimationChannel channel : profile.channels()) {
            if (channel.clip(action).isEmpty()) {
                continue;
            }
            animatable.stopTriggeredAnim(serverPlayer, instanceId, channel.name(), action.id());
            stopped = true;
        }
        return stopped;
    }

    public static void stopAndPlayAnim(GeoItem geoItem, ItemStack itemStack, ServerPlayer serverPlayer, @Nullable String controllerName, @Nullable String animName) {
        if (controllerName == null || animName == null) return;
        long orAssignId = GeoItem.getOrAssignId(itemStack, serverPlayer.serverLevel());
        AnimatableManager<GeoAnimatable> animatableManager = geoItem.getAnimatableInstanceCache().getManagerForId(orAssignId);
        AnimationController<GeoAnimatable> gunController = animatableManager.getAnimationControllers().get(controllerName);
        if (gunController == null) return;

        if (gunController.isPlayingTriggeredAnimation()) {
            geoItem.stopTriggeredAnim(serverPlayer, orAssignId, controllerName, animName);
        }
        geoItem.triggerAnim(serverPlayer, orAssignId, controllerName, animName);
    }
}
