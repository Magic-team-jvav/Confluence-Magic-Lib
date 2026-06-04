package org.confluence.lib.common.component;

import PortLib.extensions.com.mojang.serialization.Codec.PortCodecExtension;
import PortLib.extensions.net.minecraft.nbt.TagParser.PortTagParserExtension;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.CompoundTag;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

import java.util.function.Consumer;

public record NbtComponent(CompoundTag nbt) {
    public static final Codec<NbtComponent> CODEC = PortCodecExtension.withAlternative(CompoundTag.CODEC, PortTagParserExtension.asCodec()).xmap(NbtComponent::new, NbtComponent::nbt);
    public static final PortStreamCodec<ByteBuf, NbtComponent> STREAM_CODEC = PortByteBufCodecs.COMPOUND_TAG.map(NbtComponent::new, NbtComponent::nbt);

    public boolean equals(Object o) {
        return o == this || (o instanceof NbtComponent n && nbt.equals(n.nbt()));
    }

    public int hashCode() {
        return nbt.hashCode();
    }

    public static NbtComponent create(Consumer<CompoundTag> consumer) {
        CompoundTag tag = new CompoundTag();
        consumer.accept(tag);
        return new NbtComponent(tag);
    }
}
