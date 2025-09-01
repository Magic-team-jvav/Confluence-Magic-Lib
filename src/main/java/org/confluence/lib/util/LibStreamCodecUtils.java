package org.confluence.lib.util;

import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LibStreamCodecUtils {
    public static final StreamCodec<ByteBuf, Vec2> VEC_2_STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, vec2 -> vec2.x,
            ByteBufCodecs.FLOAT, vec2 -> vec2.y,
            Vec2::new
    );

    public static <B, V> StreamCodec<B, V> unit(Supplier<V> expectedValue) {
        return new StreamCodec<>() {
            @Override
            public V decode(B buffer) {
                return expectedValue.get();
            }

            @Override
            public void encode(B buffer, V value) {
                if (!value.equals(expectedValue.get())) {
                    throw new IllegalStateException("Can't encode '" + value + "', expected '" + expectedValue.get() + "'");
                }
            }
        };
    }

    public static <B extends ByteBuf, TA, TB> StreamCodec<B, Tuple<TA, TB>> tuple(StreamCodec<? super B, TA> aCodec, StreamCodec<? super B, TB> bCodec) {
        return StreamCodec.composite(aCodec, Tuple::getA, bCodec, Tuple::getB, Tuple::new);
    }

    public static <B extends ByteBuf, K, V> StreamCodec<B, Map<K, V>> map(IntFunction<Map<K, V>> factory, StreamCodec<? super B, K> keyCodec, StreamCodec<? super B, V> valueCodec) {
        return ByteBufCodecs.map(factory, keyCodec, valueCodec);
    }
}
