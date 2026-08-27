package org.confluence.lib.integration.animation;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;
import org.mesdag.particlestorm.api.geckolib.WithCurrentEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.instance.InstancedAnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;

import java.util.ArrayList;
import java.util.List;

public class PlayerGeoAnimatable implements GeoAnimatable {
    public static final List<Runnable> reloadCallbacks = new ArrayList<>();

    public final AbstractClientPlayer player;
    public final PlayerGeoModel model = new PlayerGeoModel();
    public final PlayerAnimationState state = new PlayerAnimationState(this);
    protected final AnimatableInstanceCache cache = new InstancedAnimatableInstanceCache(this);
    protected PlayerModel<AbstractClientPlayer> vanillaModel;

    protected @Nullable AddPlayerGeoModelEvent.Group currentGroup;

    public PlayerGeoAnimatable(AbstractClientPlayer player) {
        this.player = player;
        reloadCallbacks.add(() -> this.vanillaModel = null);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, state -> {
            if (currentGroup != null) {
                return currentGroup.handler().handle(state);
            }
            return PlayState.STOP;
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return player.tickCount;
    }

    public AnimatableManager<PlayerGeoAnimatable> getManager() {
        return cache.getManagerForId(player.getId());
    }

    public void reset() {
        model.reset(getVanillaModel());
    }

    public void handleAnimations(float partialTick) {
        this.currentGroup = null;
        for (var entry : AddPlayerGeoModelEvent.getGroups()) {
            AddPlayerGeoModelEvent.Group group = entry.getValue().apply(player);
            if (group != null) {
                this.currentGroup = group;
                break;
            }
        }
        if (currentGroup != null) {
            model.getBakedModel(model.getModelResource(this)); // update processor
            state.update(partialTick);
            model.handleAnimations(this, player.getId(), state, partialTick);
            model.update(state, getVanillaModel());
        }
    }

    public PlayerModel<AbstractClientPlayer> getVanillaModel() {
        if (vanillaModel == null) {
            this.vanillaModel = ((PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player)).getModel();
        }
        return vanillaModel;
    }

    private static class WithParticle extends PlayerGeoAnimatable implements WithCurrentEntity {
        private WithParticle(AbstractClientPlayer player) {
            super(player);
        }

        @Override
        public Entity getCurrentEntity() {
            return player;
        }

        @Override
        public void setCurrentEntity(Entity entity) {
            throw new UnsupportedOperationException();
        }
    }

    private static class Holder {
        private static PlayerGeoAnimatable withParticle(AbstractClientPlayer player) {
            return new WithParticle(player);
        }
    }

    public static PlayerGeoAnimatable choose(AbstractClientPlayer player) {
        if (AnimationConstants.WITH_PARTICLE) {
            return Holder.withParticle(player);
        }
        return new PlayerGeoAnimatable(player);
    }
}
