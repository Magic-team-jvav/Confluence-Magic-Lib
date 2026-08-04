package org.confluence.lib.network;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.confluence.lib.ConfluenceMagicLib;
import org.confluence.lib.common.menu.IMultiRecipeMenu;

import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public record SyncMultiRecipesPacketS2C(RecipeType<?> recipeType, List<RecipeHolder<?>> recipes) implements IPacketS2C {
    public static final Type<SyncMultiRecipesPacketS2C> TYPE = new Type<>(ConfluenceMagicLib.asConfluenceResource("sync_multi_recipes"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncMultiRecipesPacketS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.registry(Registries.RECIPE_TYPE), SyncMultiRecipesPacketS2C::recipeType,
            RecipeHolder.STREAM_CODEC.apply(ByteBufCodecs.list()), SyncMultiRecipesPacketS2C::recipes,
            SyncMultiRecipesPacketS2C::new
    );

    @Override
    public void work(Player player) {
        if (player.containerMenu instanceof IMultiRecipeMenu menu && menu.getRecipeType() == recipeType) {
            menu.setRecipes(menu.getRecipes());
        }
    }

    @Override
    public Type<SyncMultiRecipesPacketS2C> type() {
        return TYPE;
    }

    public static Packet<? super ClientGamePacketListener> makePacket(IMultiRecipeMenu menu) {
        return new SyncMultiRecipesPacketS2C(menu.getRecipeType(), menu.getRecipes()).toVanillaClientbound();
    }
}
