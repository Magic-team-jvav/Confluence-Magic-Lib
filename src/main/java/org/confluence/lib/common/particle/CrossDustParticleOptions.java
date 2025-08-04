package org.confluence.lib.common.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import org.confluence.lib.ConfluenceMagicLib;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.joml.Vector4f;

import javax.annotation.ParametersAreNonnullByDefault;

@Getter
@EqualsAndHashCode
@ToString
public final class CrossDustParticleOptions implements ParticleOptions {
    private final boolean large;
    /** argb */
    private final int centerColor;
    /** argb */
    private final int edgeColor;
    private final Vector3f velocity;
    private final Vector4f speedCurve;
    private final float scale;
    private final int lifetime;
    private final int roll;
    private final Vector4f rollCurve;
    private final boolean noGravity;
    private final boolean noPhysics;
    private final boolean fullBrightness;
    private final boolean pulse;
    private final int flags;

    public static final int FLAG_LARGE = 0b00001;
    public static final int FLAG_NO_GRAVITY = 0b00010;
    public static final int FLAG_NO_PHYSICS = 0b00100;
    public static final int FLAG_FULL_BRIGHTNESS = 0b01000;
    public static final int FLAG_PULSE = 0b10000;

    public static final MapCodec<CrossDustParticleOptions> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("centerColor").forGetter(thisOptions -> thisOptions.centerColor),
        Codec.INT.fieldOf("edgeColor").forGetter(thisOptions -> thisOptions.edgeColor),
        ExtraCodecs.VECTOR3F.fieldOf("velocity").forGetter(thisOptions -> thisOptions.velocity),
        ExtraCodecs.VECTOR4F.fieldOf("speedCurve").forGetter(thisOptions -> thisOptions.speedCurve),
        Codec.FLOAT.fieldOf("scale").forGetter(thisOptions -> thisOptions.scale),
        Codec.INT.fieldOf("lifetime").forGetter(thisOptions -> thisOptions.lifetime),
        Codec.INT.fieldOf("roll").forGetter(thisOptions -> thisOptions.roll),
        ExtraCodecs.VECTOR4F.fieldOf("rollCurve").forGetter(thisOptions -> thisOptions.rollCurve),
        Codec.INT.fieldOf("flags").forGetter(thisOptions -> thisOptions.flags)
    ).apply(instance, CrossDustParticleOptions::new));

    private static final StreamCodec<ByteBuf, Vector4f> VEC4F_CODEC = new StreamCodec<>() {
        @NotNull
        public Vector4f decode(ByteBuf buffer) {
            return new Vector4f(buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        }

        public void encode(ByteBuf buffer, Vector4f vector4f) {
            buffer.writeFloat(vector4f.x());
            buffer.writeFloat(vector4f.y());
            buffer.writeFloat(vector4f.z());
            buffer.writeFloat(vector4f.w());
        }
    };

    @ParametersAreNonnullByDefault
    @MethodsReturnNonnullByDefault
    public static final StreamCodec<FriendlyByteBuf, CrossDustParticleOptions> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CrossDustParticleOptions decode(FriendlyByteBuf buffer) {
            int centerColor = ByteBufCodecs.INT.decode(buffer);
            int edgeColor = ByteBufCodecs.INT.decode(buffer);
            Vector3f velocity = ByteBufCodecs.VECTOR3F.decode(buffer);
            Vector4f speedCurve = VEC4F_CODEC.decode(buffer);
            float scale = ByteBufCodecs.FLOAT.decode(buffer);
            int lifetime = ByteBufCodecs.VAR_INT.decode(buffer);
            int roll = ByteBufCodecs.VAR_INT.decode(buffer);
            Vector4f rollCurve = VEC4F_CODEC.decode(buffer);
            int flags = ByteBufCodecs.VAR_INT.decode(buffer);
            return new CrossDustParticleOptions(centerColor, edgeColor, velocity, speedCurve, scale, lifetime, roll, rollCurve, flags);
        }

        @Override
        public void encode(FriendlyByteBuf buffer, CrossDustParticleOptions value) {
            ByteBufCodecs.INT.encode(buffer, value.centerColor);
            ByteBufCodecs.INT.encode(buffer, value.edgeColor);
            ByteBufCodecs.VECTOR3F.encode(buffer, value.velocity);
            VEC4F_CODEC.encode(buffer, value.speedCurve);
            ByteBufCodecs.FLOAT.encode(buffer, value.scale);
            ByteBufCodecs.VAR_INT.encode(buffer, value.lifetime);
            ByteBufCodecs.VAR_INT.encode(buffer, value.roll);
            VEC4F_CODEC.encode(buffer, value.rollCurve);
            ByteBufCodecs.VAR_INT.encode(buffer, value.flags);
        }
    };


    public CrossDustParticleOptions(boolean large, int centerColor, int edgeColor, Vector3f velocity, Vector4f speedCurve, float scale, int lifetime, int roll, Vector4f rollCurve, boolean noGravity, boolean noPhysics, boolean fullBrightness, boolean pulse) {
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
        if(large) flag |= FLAG_LARGE;
        if(noGravity) flag |= FLAG_NO_GRAVITY;
        if(noPhysics) flag |= FLAG_NO_PHYSICS;
        if(fullBrightness) flag |= FLAG_FULL_BRIGHTNESS;
        if(pulse) flag |= FLAG_PULSE;
        this.flags = flag;
    }

    public CrossDustParticleOptions(int centerColor, int edgeColor, Vector3f velocity, Vector4f speedCurve, float scale, int lifetime, int roll, Vector4f rollCurve, int flags) {
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

    @Override
    @NotNull
    public ParticleType<?> getType() {
        return ConfluenceMagicLib.CROSS_DUST_PARTICLE.get();
    }
}
