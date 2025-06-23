package org.confluence.lib.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.Ingredient;
import org.confluence.lib.common.recipe.AmountIngredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ExtraByteBufCodecs {
    StreamCodec<ByteBuf, Long> LONG = new StreamCodec<>() {
        public Long decode(ByteBuf buffer) {
            return buffer.readLong();
        }

        public void encode(ByteBuf buffer, Long value) {
            buffer.writeLong(value);
        }
    };
    StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS = new StreamCodec<>() {
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
    StreamCodec<? super FriendlyByteBuf, java.util.UUID> UUID = new StreamCodec<>() {
        @Override
        public java.util.UUID decode(FriendlyByteBuf buffer) {
            return buffer.readUUID();
        }

        @Override
        public void encode(FriendlyByteBuf buffer, java.util.UUID value) {
            buffer.writeUUID(value);
        }
    };

    static <V1, V2, B extends ByteBuf> StreamCodec<B, Tuple<V1, V2>> tuple(StreamCodec<? super B, V1> codecA, StreamCodec<? super B, V2> codecB) {
        return new StreamCodec<>() {
            @Override
            public Tuple<V1, V2> decode(B buffer) {
                return new Tuple<>(codecA.decode(buffer), codecB.decode(buffer));
            }

            @Override
            public void encode(B buffer, Tuple<V1, V2> value) {
                codecA.encode(buffer, value.getA());
                codecB.encode(buffer, value.getB());
            }
        };
    }

    static <V, B extends ByteBuf> StreamCodec<B, List<V>> listOf(StreamCodec<B, V> codec) {
        return new StreamCodec<>() {
            @Override
            public List<V> decode(B buffer) {
                int size = buffer.readInt();
                List<V> list = new ArrayList<>();
                for (int i = 0; i < size; i++) {
                    list.add(codec.decode(buffer));
                }
                return list;
            }

            @Override
            public void encode(B buffer, List<V> value) {
                buffer.writeInt(value.size());
                for (V v : value) {
                    codec.encode(buffer, v);
                }
            }
        };
    }

    static <T, B extends ByteBuf> StreamCodec<B, TagKey<T>> tagKey(ResourceKey<Registry<T>> resourceKey) {
        return new StreamCodec<>() {
            public TagKey<T> decode(B buffer) {
                return TagKey.create(resourceKey, ResourceLocation.STREAM_CODEC.decode(buffer));
            }

            public void encode(B buffer, TagKey<T> tagKey) {
                ResourceLocation.STREAM_CODEC.encode(buffer, tagKey.location());
            }
        };
    }
}
