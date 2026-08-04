package org.confluence.lib.client.render.item.properties;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.item.IFunctionCouldEnable;
import org.confluence.lib.util.LibUtils;
import org.jspecify.annotations.Nullable;

public enum FunctionEnabled implements ConditionalItemModelProperty {
    INSTANCE;

    public static final Identifier ID = ConfluenceMagicLib.asResource("function_enabled");
    public static final MapCodec<FunctionEnabled> CODEC = MapCodec.unit(INSTANCE);

    @Override
    public MapCodec<FunctionEnabled> type() {
        return CODEC;
    }

    @Override
    public boolean get(ItemStack itemStack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
        CompoundTag tag = LibUtils.getItemStackNbtIfPresent(itemStack);
        return tag == null || !tag.getBooleanOr(IFunctionCouldEnable.DISABLE_KEY, false);
    }
}
