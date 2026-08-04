package org.confluence.lib.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.confluence.lib.common.particle.CrossDustParticleOptions;
import org.confluence.lib.util.LibMathUtils;
import org.jetbrains.annotations.Nullable;

public class CrossDustParticle extends SingleQuadParticle {
    protected final SpriteSet sprites;
    protected final CrossDustParticleOptions options;
    protected float quadSizeOld;
    protected float accel;
    protected float accelOld;
    protected float rollDelta;
    protected float rollDeltaOld;

    public CrossDustParticle(ClientLevel level, double x, double y, double z, CrossDustParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z, sprites.first());
        this.sprites = sprites;
        this.options = options;
        this.lifetime = options.lifetime;
        this.gravity = options.noGravity ? 0 : 0.7F;
        this.hasPhysics = !options.noPhysics;
        this.quadSize = 0.1F;
        setScale(options.pulse ? 0 : options.scale);
        this.quadSizeOld = quadSize;
        this.xd = options.velocity.x();
        this.yd = options.velocity.y();
        this.zd = options.velocity.z();
        this.age = 1;
    }

    @Override
    public void extract(QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {
        useCenterSprite();
        super.extract(particleTypeRenderState, camera, partialTickTime);
        useEdgeSprite();
        super.extract(particleTypeRenderState, camera, partialTickTime);
    }

    @Override
    public void tick() {
        float lastProgress = ((float) age - 1) / lifetime;
        float progress = (float) age / lifetime;
        if (options.noGravity) {
            this.accelOld = accel;
            this.accel = LibMathUtils.cubicBezier(progress, options.speedCurve.x(), options.speedCurve.y(), options.speedCurve.z(), options.speedCurve.w());
            float k = (accel - accelOld) / (progress - lastProgress);
            this.xd = options.velocity.x() * k;
            this.yd = options.velocity.y() * k;
            this.zd = options.velocity.z() * k;
        }
        this.oRoll = roll;
        this.rollDeltaOld = rollDelta;
        this.rollDelta = LibMathUtils.cubicBezier(progress, options.rollCurve.x(), options.rollCurve.y(), options.rollCurve.z(), options.rollCurve.w());
        float k = (rollDelta - rollDeltaOld) / (progress - lastProgress);
        this.roll += options.roll * k * Mth.DEG_TO_RAD;

        this.quadSizeOld = quadSize;
        if (options.pulse) {
            if (progress < 0.5f) {
                setScale(options.scale * progress * 2);
            } else {
                setScale(options.scale * (1 - progress) * 2);
            }
        } else {
            setScale(options.scale * (1 - progress));
        }

        super.tick();
    }

    public void setScale(float scale) {
        this.quadSize = scale * 0.1F;
        scale = quadSize + quadSize;
        setSize(scale, scale);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return Mth.lerp(partialTicks, quadSizeOld, quadSize);
    }

    protected void useCenterSprite() {
        setSprite(sprites.get(options.large ? 3 : 1, 4));
        this.alpha = (options.centerColor >>> 24 & 0xFF) / 255F;
        this.rCol = (options.centerColor >>> 16 & 0xFF) / 255F;
        this.gCol = (options.centerColor >>> 8 & 0xFF) / 255F;
        this.bCol = (options.centerColor & 0xFF) / 255F;
    }

    protected void useEdgeSprite() {
        setSprite(sprites.get(options.large ? 4 : 2, 4));
        this.alpha = (options.edgeColor >>> 24 & 0xFF) / 255F;
        this.rCol = (options.edgeColor >>> 16 & 0xFF) / 255F;
        this.gCol = (options.edgeColor >>> 8 & 0xFF) / 255F;
        this.bCol = (options.edgeColor & 0xFF) / 255F;
    }

    @Override
    protected int getLightCoords(float partialTick) {
        if (options.fullBrightness) {
            return 0xF000F0;
        }
        return super.getLightCoords(partialTick);
    }

    @Override
    protected Layer getLayer() {
        return Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<CrossDustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(CrossDustParticleOptions options, ClientLevel level, double x, double y, double z, double xAux, double yAux, double zAux, RandomSource random) {
            return new CrossDustParticle(level, x, y, z, options, sprites);
        }
    }
}
