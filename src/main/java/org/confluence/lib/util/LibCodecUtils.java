package org.confluence.lib.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

public final class LibCodecUtils {
    public static final Codec<Vec2> VEC_2 = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("x").forGetter(vec2 -> vec2.x),
            Codec.FLOAT.fieldOf("y").forGetter(vec2 -> vec2.y)
    ).apply(instance, Vec2::new));

    public static <A, B> Codec<Tuple<A, B>> tuple(Codec<A> aCodec, Codec<B> bCodec) {
        return tuple("a", aCodec, "b", bCodec);
    }

    public static <A, B> Codec<Tuple<A, B>> tuple(String aName, Codec<A> aCodec, String bName, Codec<B> bCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                aCodec.fieldOf(aName).forGetter(Tuple::getA),
                bCodec.fieldOf(bName).forGetter(Tuple::getB)
        ).apply(instance, Tuple::new));
    }

    public static <L, M, R> Codec<Triple<L, M, R>> triple(Codec<L> lCodec, Codec<M> mCodec, Codec<R> rCodec) {
        return triple("l", lCodec, "m", mCodec, "r", rCodec);
    }

    public static <L, M, R> Codec<Triple<L, M, R>> triple(String lName, Codec<L> lCodec, String mName, Codec<M> mCodec, String rName, Codec<R> rCodec) {
        return RecordCodecBuilder.create(instance -> instance.group(
                lCodec.fieldOf(lName).forGetter(Triple::getLeft),
                mCodec.fieldOf(mName).forGetter(Triple::getMiddle),
                rCodec.fieldOf(rName).forGetter(Triple::getRight)
        ).apply(instance, ImmutableTriple::new));
    }
}
