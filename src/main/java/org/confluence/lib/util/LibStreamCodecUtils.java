package org.confluence.lib.util;

import PortLib.extensions.net.minecraft.resources.ResourceLocation.PortResourceLocationExtension;
import PortLib.extensions.net.minecraft.world.item.crafting.Ingredient.PortIngredientExtension;
import com.mojang.datafixers.util.*;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.booleans.BooleanObjectMutablePair;
import it.unimi.dsi.fastutil.booleans.BooleanObjectPair;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;
import org.confluence.lib.common.recipe.AmountIngredient;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.Map;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public final class LibStreamCodecUtils {
    public static final PortStreamCodec<ByteBuf, Vec2> VEC_2 = PortStreamCodec.composite(
            PortByteBufCodecs.FLOAT, vec2 -> vec2.x,
            PortByteBufCodecs.FLOAT, vec2 -> vec2.y,
            Vec2::new
    );
    public static final PortStreamCodec<FriendlyByteBuf, java.util.UUID> UUID = new PortStreamCodec<>() {
        @Override
        public java.util.UUID decode(FriendlyByteBuf buffer) {
            return buffer.readUUID();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, java.util.UUID value) {
            buffer.writeUUID(value);
        }
    };
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS = new PortStreamCodec<>() {
        @Override
        public NonNullList<Ingredient> decode(PortRegistryFriendlyByteBuf buffer) {
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(buffer.readVarInt(), AmountIngredient.EMPTY);
            nonnulllist.replaceAll(ignore -> PortIngredientExtension.contentsStreamCodec().decode(buffer));
            return nonnulllist;
        }

        @Override
        public void encode(PortRegistryFriendlyByteBuf buffer, NonNullList<Ingredient> value) {
            buffer.writeVarInt(value.size());
            for (Ingredient ingredient : value) {
                PortIngredientExtension.contentsStreamCodec().encode(buffer, ingredient);
            }
        }
    };
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, BlockState> BLOCK_STATE = new PortStreamCodec<>() {
        private final PortStreamCodec<PortRegistryFriendlyByteBuf, Block> blockCodec = PortByteBufCodecs.registry(Registries.BLOCK);

        @Override
        public BlockState decode(PortRegistryFriendlyByteBuf buffer) {
            Block block = blockCodec.decode(buffer);
            return block.getStateDefinition().getPossibleStates().get(buffer.readVarInt());
        }

        @Override
        public void encode(PortRegistryFriendlyByteBuf buffer, BlockState value) {
            Block block = value.getBlock();
            blockCodec.encode(buffer, block);
            buffer.writeVarInt(block.getStateDefinition().getPossibleStates().indexOf(value));
        }
    };

    public static <B extends ByteBuf, V> PortStreamCodec<B, V> unit(Supplier<V> expectedValue) {
        return new PortStreamCodec<>() {
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

    public static <B extends ByteBuf, TA, TB> PortStreamCodec<B, Tuple<TA, TB>> tuple(PortStreamCodec<? super B, TA> aCodec, PortStreamCodec<? super B, TB> bCodec) {
        return PortStreamCodec.composite(aCodec, Tuple::getA, bCodec, Tuple::getB, Tuple::new);
    }

    public static <B extends ByteBuf, L, M, R> PortStreamCodec<B, Triple<L, M, R>> triple(PortStreamCodec<? super B, L> lCodec, PortStreamCodec<? super B, M> mCodec, PortStreamCodec<? super B, R> rCodec) {
        return PortStreamCodec.composite(lCodec, Triple::getLeft, mCodec, Triple::getMiddle, rCodec, Triple::getRight, ImmutableTriple::new);
    }

    public static <B extends ByteBuf, K, V> PortStreamCodec<B, Map<K, V>> map(IntFunction<Map<K, V>> factory, PortStreamCodec<? super B, K> keyCodec, PortStreamCodec<? super B, V> valueCodec) {
        return PortByteBufCodecs.map(factory, keyCodec, valueCodec);
    }

    public static <B extends ByteBuf, V> PortStreamCodec<B, Object2BooleanMap<V>> object2BooleanMap(PortStreamCodec<? super B, V> codec) {
        return PortByteBufCodecs.map(Object2BooleanOpenHashMap::new, codec, PortByteBufCodecs.BOOL);
    }

    public static <B extends ByteBuf, V> PortStreamCodec<B, TagKey<V>> tagKey(ResourceKey<Registry<V>> resourceKey) {
        return new PortStreamCodec<>() {
            public TagKey<V> decode(B buffer) {
                return TagKey.create(resourceKey, PortResourceLocationExtension.streamCodec().decode(buffer));
            }

            public void encode(B buffer, TagKey<V> tagKey) {
                PortResourceLocationExtension.streamCodec().encode(buffer, tagKey.location());
            }
        };
    }

    public static <B extends ByteBuf, V> PortStreamCodec<B, V> lazyInitialized(Supplier<PortStreamCodec<B, V>> delegate) {
        return new PortStreamCodec<>() {
            @Override
            public V decode(B buffer) {
                return delegate.get().decode(buffer);
            }

            @Override
            public void encode(B buffer, V value) {
                delegate.get().encode(buffer, value);
            }
        };
    }

    public static PortStreamCodec<ByteBuf, boolean[]> booleanArray(int size) {
        int length = size % 8 == 0 ? size / 8 : size / 8 + 1;
        return new PortStreamCodec<>() {
            @Override
            public boolean[] decode(ByteBuf buffer) {
                boolean[] result = new boolean[size];
                for (int i = 0; i < length; i++) {
                    byte b = buffer.readByte();
                    int startIndex = i * 8;

                    for (int j = 0; j < 8; j++) {
                        int index = startIndex + j;
                        if (index < size) {
                            result[index] = (b & (1 << j)) != 0;
                        }
                    }
                }
                return result;
            }

            @Override
            public void encode(ByteBuf buffer, boolean[] value) {
                if (value.length != size) {
                    throw new IllegalArgumentException("Boolean array size mismatch. Expected: " + size + ", actual: " + value.length);
                }

                for (int i = 0; i < length; i++) {
                    byte b = 0;
                    int startIndex = i * 8;

                    for (int j = 0; j < 8; j++) {
                        int index = startIndex + j;
                        if (index < value.length && value[index]) {
                            b |= (byte) (1 << j);
                        }
                    }

                    buffer.writeByte(b);
                }
            }
        };
    }

    public static <B extends ByteBuf, O> PortStreamCodec<B, BooleanObjectPair<O>> booleanObjectPair(PortStreamCodec<? super B, O> objCodec) {
        return new PortStreamCodec<>() {
            @Override
            public BooleanObjectPair<O> decode(B buffer) {
                return new BooleanObjectMutablePair<>(buffer.readBoolean(), objCodec.decode(buffer));
            }

            @Override
            public void encode(B buffer, BooleanObjectPair<O> value) {
                buffer.writeBoolean(value.leftBoolean());
                objCodec.encode(buffer, value.right());
            }
        };
    }

    public static <E extends Enum<E>> PortStreamCodec<FriendlyByteBuf, E> fromEnum(E[] values) {
        return new PortStreamCodec<>() {
            @Override
            public E decode(FriendlyByteBuf buffer) {
                return values[buffer.readVarInt()];
            }

            @Override
            public void encode(FriendlyByteBuf buffer, E value) {
                buffer.writeVarInt(value.ordinal());
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final Function7<T1, T2, T3, T4, T5, T6, T7, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final Function8<T1, T2, T3, T4, T5, T6, T7, T8, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final Function9<T1, T2, T3, T4, T5, T6, T7, T8, T9, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final Function10<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final Function11<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final PortStreamCodec<? super B, T12> codec12,
            final Function<C, T12> getter12,
            final Function12<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
                codec12.encode(buffer, getter12.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final PortStreamCodec<? super B, T12> codec12,
            final Function<C, T12> getter12,
            final PortStreamCodec<? super B, T13> codec13,
            final Function<C, T13> getter13,
            final Function13<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
                codec12.encode(buffer, getter12.apply(composite));
                codec13.encode(buffer, getter13.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final PortStreamCodec<? super B, T12> codec12,
            final Function<C, T12> getter12,
            final PortStreamCodec<? super B, T13> codec13,
            final Function<C, T13> getter13,
            final PortStreamCodec<? super B, T14> codec14,
            final Function<C, T14> getter14,
            final Function14<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
                codec12.encode(buffer, getter12.apply(composite));
                codec13.encode(buffer, getter13.apply(composite));
                codec14.encode(buffer, getter14.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final PortStreamCodec<? super B, T12> codec12,
            final Function<C, T12> getter12,
            final PortStreamCodec<? super B, T13> codec13,
            final Function<C, T13> getter13,
            final PortStreamCodec<? super B, T14> codec14,
            final Function<C, T14> getter14,
            final PortStreamCodec<? super B, T15> codec15,
            final Function<C, T15> getter15,
            final Function15<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                T15 t15 = codec15.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
                codec12.encode(buffer, getter12.apply(composite));
                codec13.encode(buffer, getter13.apply(composite));
                codec14.encode(buffer, getter14.apply(composite));
                codec15.encode(buffer, getter15.apply(composite));
            }
        };
    }

    public static <B, C, T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16> PortStreamCodec<B, C> composite(
            final PortStreamCodec<? super B, T1> codec1,
            final Function<C, T1> getter1,
            final PortStreamCodec<? super B, T2> codec2,
            final Function<C, T2> getter2,
            final PortStreamCodec<? super B, T3> codec3,
            final Function<C, T3> getter3,
            final PortStreamCodec<? super B, T4> codec4,
            final Function<C, T4> getter4,
            final PortStreamCodec<? super B, T5> codec5,
            final Function<C, T5> getter5,
            final PortStreamCodec<? super B, T6> codec6,
            final Function<C, T6> getter6,
            final PortStreamCodec<? super B, T7> codec7,
            final Function<C, T7> getter7,
            final PortStreamCodec<? super B, T8> codec8,
            final Function<C, T8> getter8,
            final PortStreamCodec<? super B, T9> codec9,
            final Function<C, T9> getter9,
            final PortStreamCodec<? super B, T10> codec10,
            final Function<C, T10> getter10,
            final PortStreamCodec<? super B, T11> codec11,
            final Function<C, T11> getter11,
            final PortStreamCodec<? super B, T12> codec12,
            final Function<C, T12> getter12,
            final PortStreamCodec<? super B, T13> codec13,
            final Function<C, T13> getter13,
            final PortStreamCodec<? super B, T14> codec14,
            final Function<C, T14> getter14,
            final PortStreamCodec<? super B, T15> codec15,
            final Function<C, T15> getter15,
            final PortStreamCodec<? super B, T16> codec16,
            final Function<C, T16> getter16,
            final Function16<T1, T2, T3, T4, T5, T6, T7, T8, T9, T10, T11, T12, T13, T14, T15, T16, C> factory
    ) {
        return new PortStreamCodec<>() {
            @Override
            public C decode(B buffer) {
                T1 t1 = codec1.decode(buffer);
                T2 t2 = codec2.decode(buffer);
                T3 t3 = codec3.decode(buffer);
                T4 t4 = codec4.decode(buffer);
                T5 t5 = codec5.decode(buffer);
                T6 t6 = codec6.decode(buffer);
                T7 t7 = codec7.decode(buffer);
                T8 t8 = codec8.decode(buffer);
                T9 t9 = codec9.decode(buffer);
                T10 t10 = codec10.decode(buffer);
                T11 t11 = codec11.decode(buffer);
                T12 t12 = codec12.decode(buffer);
                T13 t13 = codec13.decode(buffer);
                T14 t14 = codec14.decode(buffer);
                T15 t15 = codec15.decode(buffer);
                T16 t16 = codec16.decode(buffer);
                return factory.apply(t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16);
            }

            @Override
            public void encode(B buffer, C composite) {
                codec1.encode(buffer, getter1.apply(composite));
                codec2.encode(buffer, getter2.apply(composite));
                codec3.encode(buffer, getter3.apply(composite));
                codec4.encode(buffer, getter4.apply(composite));
                codec5.encode(buffer, getter5.apply(composite));
                codec6.encode(buffer, getter6.apply(composite));
                codec7.encode(buffer, getter7.apply(composite));
                codec8.encode(buffer, getter8.apply(composite));
                codec9.encode(buffer, getter9.apply(composite));
                codec10.encode(buffer, getter10.apply(composite));
                codec11.encode(buffer, getter11.apply(composite));
                codec12.encode(buffer, getter12.apply(composite));
                codec13.encode(buffer, getter13.apply(composite));
                codec14.encode(buffer, getter14.apply(composite));
                codec15.encode(buffer, getter15.apply(composite));
                codec16.encode(buffer, getter16.apply(composite));
            }
        };
    }
}
