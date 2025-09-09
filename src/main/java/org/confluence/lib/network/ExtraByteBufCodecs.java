package org.confluence.lib.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.crafting.Ingredient;
import org.confluence.lib.util.LibStreamCodecUtils;
import org.jetbrains.annotations.ApiStatus;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@Deprecated(forRemoval = true)
@ApiStatus.ScheduledForRemoval(inVersion = "1.3.0")
public interface ExtraByteBufCodecs {
    StreamCodec<ByteBuf, Long> LONG = new StreamCodec<>() {
        public Long decode(ByteBuf buffer) {
            return buffer.readLong();
        }

        public void encode(ByteBuf buffer, Long value) {
            buffer.writeLong(value);
        }
    };
    StreamCodec<RegistryFriendlyByteBuf, NonNullList<Ingredient>> INGREDIENTS = LibStreamCodecUtils.INGREDIENTS;
    StreamCodec<? super FriendlyByteBuf, java.util.UUID> UUID = LibStreamCodecUtils.UUID;

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
        return LibStreamCodecUtils.tagKey(resourceKey);
    }
}
