package org.confluence.lib.common.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.mesdag.portlib.network.PortRegistryFriendlyByteBuf;
import org.mesdag.portlib.network.codec.PortByteBufCodecs;
import org.mesdag.portlib.network.codec.PortStreamCodec;

public record ToolMode(int mode) {
    public static final Codec<ToolMode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("mode").forGetter(ToolMode::mode)
    ).apply(instance, ToolMode::new));
    public static final PortStreamCodec<PortRegistryFriendlyByteBuf, ToolMode> STREAM_CODEC = PortStreamCodec.composite(
            PortByteBufCodecs.INT, ToolMode::mode,
            ToolMode::new
    );

    public boolean equals(Object o) {
        return o == this || (o instanceof ToolMode t && mode == t.mode());
    }

    public int hashCode() {
        return mode;
    }
}
