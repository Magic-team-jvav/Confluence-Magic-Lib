package org.confluence.lib.util;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2BooleanMap;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec2;
import org.confluence.lib.common.recipe.AmountIngredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Map;
import java.util.function.IntFunction;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class LibStreamCodecUtils {
    public static final StreamCodec<ByteBuf, Vec2> VEC_2 = StreamCodec.composite(
            ByteBufCodecs.FLOAT, vec2 -> vec2.x,
            ByteBufCodecs.FLOAT, vec2 -> vec2.y,
            Vec2::new
    );
    public static final StreamCodec<FriendlyByteBuf, java.util.UUID> UUID = new StreamCodec<>() {
        @Override
        public java.util.UUID decode(FriendlyByteBuf buffer) {
            return buffer.readUUID();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, java.util.UUID value) {
            buffer.writeUUID(value);
        }
    };
    public static final StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS = new StreamCodec<>() {
        @Override
        public NonNullList<Ingredient> decode(RegistryFriendlyByteBuf buffer) {
            NonNullList<Ingredient> nonnulllist = NonNullList.withSize(buffer.readVarInt(), AmountIngredient.EMPTY);
            nonnulllist.replaceAll(ignore -> Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            return nonnulllist;
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, NonNullList<Ingredient> value) {
            buffer.writeVarInt(value.size());
            for (Ingredient ingredient : value) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient);
            }
        }
    };

    public static <B extends ByteBuf, V> StreamCodec<B, V> unit(Supplier<V> expectedValue) {
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

    public static <B extends ByteBuf, V> StreamCodec<B, Object2BooleanMap<V>> object2BooleanMap(StreamCodec<? super B, V> codec) {
        return ByteBufCodecs.map(Object2BooleanOpenHashMap::new, codec, ByteBufCodecs.BOOL);
    }

    public static <B extends ByteBuf, V> StreamCodec<B, TagKey<V>> tagKey(ResourceKey<Registry<V>> resourceKey) {
        return new StreamCodec<>() {
            public TagKey<V> decode(B buffer) {
                return TagKey.create(resourceKey, ResourceLocation.STREAM_CODEC.decode(buffer));
            }

            public void encode(B buffer, TagKey<V> tagKey) {
                ResourceLocation.STREAM_CODEC.encode(buffer, tagKey.location());
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <B extends ByteBuf, V> StreamCodec<B, V> registry(Registry<V> registry) {
        return (StreamCodec<B, V>) ResourceLocation.STREAM_CODEC.map(registry::get, registry::getKey);
    }

    public static <B extends ByteBuf, V> StreamCodec<B, V> lazyInitialized(Supplier<StreamCodec<B, V>> delegate) {
        return new StreamCodec<>() {
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
}
