package org.confluence.lib.common.particle;

import PortLib.extensions.com.mojang.serialization.Codec.PortCodecExtension;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.confluence.lib.ConfluenceMagicLib;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;
import org.mesdag.portlib.wrapper.core.particles.PortParticleOptions;

public final class CrossDustParticleOptions extends PortParticleOptions {
    public static final int FLAG_LARGE = 0b00001;
    public static final int FLAG_NO_GRAVITY = 0b00010;
    public static final int FLAG_NO_PHYSICS = 0b00100;
    public static final int FLAG_FULL_BRIGHTNESS = 0b01000;
    public static final int FLAG_PULSE = 0b10000;

    public static final MapCodec<CrossDustParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("centerColor").forGetter(thisOptions -> thisOptions.centerColor),
            Codec.INT.fieldOf("edgeColor").forGetter(thisOptions -> thisOptions.edgeColor),
            ExtraCodecs.VECTOR3F.fieldOf("velocity").forGetter(thisOptions -> thisOptions.velocity),
            PortCodecExtension.vector4f().fieldOf("speedCurve").forGetter(thisOptions -> thisOptions.speedCurve),
            Codec.FLOAT.fieldOf("scale").forGetter(thisOptions -> thisOptions.scale),
            Codec.INT.fieldOf("lifetime").forGetter(thisOptions -> thisOptions.lifetime),
            Codec.INT.fieldOf("roll").forGetter(thisOptions -> thisOptions.roll),
            PortCodecExtension.vector4f().fieldOf("rollCurve").forGetter(thisOptions -> thisOptions.rollCurve),
            Codec.INT.fieldOf("flags").forGetter(thisOptions -> thisOptions.flags)
    ).apply(instance, CrossDustParticleOptions::new));

    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, CrossDustParticleOptions> STREAM_CODEC = new PortStreamCodec<>() {
        @Override
        public CrossDustParticleOptions decode(PortRegistryFriendlyByteBuf buffer) {
            int centerColor = PortByteBufCodecs.INT.decode(buffer);
            int edgeColor = PortByteBufCodecs.INT.decode(buffer);
            Vector3f velocity = PortByteBufCodecs.VECTOR3F.decode(buffer);
            Vector4f speedCurve = PortByteBufCodecs.VECTOR4F.decode(buffer);
            float scale = PortByteBufCodecs.FLOAT.decode(buffer);
            int lifetime = PortByteBufCodecs.VAR_INT.decode(buffer);
            int roll = PortByteBufCodecs.VAR_INT.decode(buffer);
            Vector4f rollCurve = PortByteBufCodecs.VECTOR4F.decode(buffer);
            int flags = PortByteBufCodecs.VAR_INT.decode(buffer);
            return new CrossDustParticleOptions(centerColor, edgeColor, velocity, speedCurve, scale, lifetime, roll, rollCurve, flags);
        }

        @Override
        public void encode(PortRegistryFriendlyByteBuf buffer, CrossDustParticleOptions value) {
            PortByteBufCodecs.INT.encode(buffer, value.centerColor);
            PortByteBufCodecs.INT.encode(buffer, value.edgeColor);
            PortByteBufCodecs.VECTOR3F.encode(buffer, value.velocity);
            PortByteBufCodecs.VECTOR4F.encode(buffer, value.speedCurve);
            PortByteBufCodecs.FLOAT.encode(buffer, value.scale);
            PortByteBufCodecs.VAR_INT.encode(buffer, value.lifetime);
            PortByteBufCodecs.VAR_INT.encode(buffer, value.roll);
            PortByteBufCodecs.VECTOR4F.encode(buffer, value.rollCurve);
            PortByteBufCodecs.VAR_INT.encode(buffer, value.flags);
        }
    };

    public final boolean large;
    public final int centerColor;
    public final int edgeColor;
    public final Vector3f velocity;
    public final Vector4f speedCurve;
    public final float scale;
    public final int lifetime;
    public final int roll;
    public final Vector4f rollCurve;
    public final boolean noGravity;
    public final boolean noPhysics;
    public final boolean fullBrightness;
    public final boolean pulse;
    public final int flags;

    public CrossDustParticleOptions(boolean large, int centerColor, int edgeColor, Vector3f velocity, Vector4f speedCurve, float scale, int lifetime, int roll, Vector4f rollCurve, boolean noGravity, boolean noPhysics, boolean fullBrightness, boolean pulse) {
        super(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CODEC, STREAM_CODEC);
        this.large = large;
        this.centerColor = centerColor;
        this.edgeColor = edgeColor;
        this.velocity = velocity;
        this.speedCurve = speedCurve;
        this.scale = scale;
        this.lifetime = lifetime;
        this.roll = roll;
        this.rollCurve = rollCurve;
        this.noGravity = noGravity;
        this.noPhysics = noPhysics;
        this.fullBrightness = fullBrightness;
        this.pulse = pulse;
        int flag = 0;
        if (large) flag |= FLAG_LARGE;
        if (noGravity) flag |= FLAG_NO_GRAVITY;
        if (noPhysics) flag |= FLAG_NO_PHYSICS;
        if (fullBrightness) flag |= FLAG_FULL_BRIGHTNESS;
        if (pulse) flag |= FLAG_PULSE;
        this.flags = flag;
    }

    public CrossDustParticleOptions(int centerColor, int edgeColor, Vector3f velocity, Vector4f speedCurve, float scale, int lifetime, int roll, Vector4f rollCurve, int flags) {
        super(ConfluenceMagicLib.CROSS_DUST_PARTICLE.get(), CODEC, STREAM_CODEC);
        this.centerColor = centerColor;
        this.edgeColor = edgeColor;
        this.velocity = velocity;
        this.speedCurve = speedCurve;
        this.scale = scale;
        this.lifetime = lifetime;
        this.roll = roll;
        this.rollCurve = rollCurve;
        this.flags = flags;
        this.large = (flags & FLAG_LARGE) == FLAG_LARGE;
        this.noGravity = (flags & FLAG_NO_GRAVITY) == FLAG_NO_GRAVITY;
        this.noPhysics = (flags & FLAG_NO_PHYSICS) == FLAG_NO_PHYSICS;
        this.fullBrightness = (flags & FLAG_FULL_BRIGHTNESS) == FLAG_FULL_BRIGHTNESS;
        this.pulse = (flags & FLAG_PULSE) == FLAG_PULSE;
    }
}
