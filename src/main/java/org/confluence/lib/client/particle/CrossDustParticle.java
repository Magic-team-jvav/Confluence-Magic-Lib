package org.confluence.lib.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import org.confluence.lib.common.particle.CrossDustParticleOptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CrossDustParticle extends TextureSheetParticle {
    protected final SpriteSet sprites;
    protected final CrossDustParticleOptions options;
    protected float quadSizeOld;
    protected float accel;
    protected float accelOld;
    protected float rollDelta;
    protected float rollDeltaOld;

    public CrossDustParticle(ClientLevel level, double x, double y, double z, CrossDustParticleOptions options, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.options = options;
        this.lifetime = options.getLifetime();
        this.gravity = options.isNoGravity() ? 0 : 0.7f;
        this.hasPhysics = !options.isNoPhysics();
        this.quadSize = 0.1f;
        setScale(options.isPulse() ? 0 : options.getScale());
        quadSizeOld = quadSize;
        xd = options.getVelocity().x;
        yd = options.getVelocity().y;
        zd = options.getVelocity().z;
        age = 1;
    }

    @Override
    public void render(VertexConsumer buffer, Camera renderInfo, float partialTicks) {
        useCenterSprite();
        super.render(buffer, renderInfo, partialTicks);
        useEdgeSprite();
        super.render(buffer, renderInfo, partialTicks);
    }

    @Override
    public void tick() {
        float lastProgress = ((float) age - 1) / lifetime;
        float progress = (float) age / lifetime;
        if (options.isNoGravity()) {
            accelOld = accel;
            accel = LibUtils.cubicBezier(progress, options.getSpeedCurve().x, options.getSpeedCurve().y, options.getSpeedCurve().z, options.getSpeedCurve().w);
            float k = (accel - accelOld) / (progress - lastProgress);
            xd = options.getVelocity().x * k;
            yd = options.getVelocity().y * k;
            zd = options.getVelocity().z * k;
        }
        oRoll = roll;
        rollDeltaOld = rollDelta;
        rollDelta = LibUtils.cubicBezier(progress, options.getRollCurve().x, options.getRollCurve().y, options.getRollCurve().z, options.getRollCurve().w);
        float k = (rollDelta - rollDeltaOld) / (progress - lastProgress);
        roll += options.getRoll() * k * Mth.DEG_TO_RAD;

        quadSizeOld = quadSize;
        if (options.isPulse()) {
            if (progress < 0.5f) {
                setScale(options.getScale() * progress * 2);
            } else {
                setScale(options.getScale() * (1 - progress) * 2);
            }
        } else {
            setScale(options.getScale() * (1 - progress));
        }

        super.tick();
    }

    public void setScale(float scale) {
        quadSize = scale * 0.1f;
        setSize(scale * 0.2f, scale * 0.2f);
    }

    @Override
    public float getQuadSize(float partialTicks) {
        return Mth.lerp(partialTicks, quadSizeOld, quadSize);
    }

    protected void useCenterSprite() {
        setSprite(sprites.get(options.isLarge() ? 3 : 1, 4));
        float a = (options.getCenterColor() >>> 24 & 0xff) / 255f;
        float r = (options.getCenterColor() >>> 16 & 0xff) / 255f;
        float g = (options.getCenterColor() >>> 8 & 0xff) / 255f;
        float b = (options.getCenterColor() & 0xff) / 255f;
        this.alpha = a;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    protected void useEdgeSprite() {
        setSprite(sprites.get(options.isLarge() ? 4 : 2, 4));
        float a = (options.getEdgeColor() >>> 24 & 0xff) / 255f;
        float r = (options.getEdgeColor() >>> 16 & 0xff) / 255f;
        float g = (options.getEdgeColor() >>> 8 & 0xff) / 255f;
        float b = (options.getEdgeColor() & 0xff) / 255f;
        this.alpha = a;
        this.rCol = r;
        this.gCol = g;
        this.bCol = b;
    }

    @Override
    protected int getLightColor(float partialTick) {
        if (options.isFullBrightness()) {
            return 15 << 20 | 15 << 4;
        }
        return super.getLightColor(partialTick);
    }

    @Override
    @NotNull
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<CrossDustParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        @Nullable
        public Particle createParticle(CrossDustParticleOptions type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            return new CrossDustParticle(level, x, y, z, type, sprites);
        }
    }
}
