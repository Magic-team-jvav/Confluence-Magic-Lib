package org.confluence.lib.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(FriendlyByteBuf.class)
public abstract class FriendlyByteBufMixin {
    @Shadow
    public abstract int readVarInt();

    // 消除ItemStack数量上限

    @WrapOperation(method = "writeItemStack", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;writeByte(I)Lio/netty/buffer/ByteBuf;"))
    private ByteBuf useWriteVarInt(FriendlyByteBuf instance, int value, Operation<ByteBuf> original) {
        return instance.writeVarInt(value);
    }

    @WrapOperation(method = "readItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/FriendlyByteBuf;readByte()B"))
    private byte cancelReadByte(FriendlyByteBuf instance, Operation<Byte> original) {
        return 0;
    }

    @ModifyVariable(method = "readItem", at = @At("STORE"), name = "i")
    private int useReadVarInt(int i) {
        return readVarInt();
    }
}
