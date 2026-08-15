package org.confluence.lib.common;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.confluence.lib.util.LibUtils;
import org.mesdag.portlib.wrapper.IPortNBTSerializable;

import javax.annotation.Nullable;

public class PlayerContainer<C extends BlockEntity & PlayerContainer.ValidEntity> extends SimpleContainer implements IPortNBTSerializable<ListTag> {
    @Nullable
    protected C activeContainer;

    public PlayerContainer(int rows) {
        super(9 * rows);
    }

    public void setActiveContainer(@Nullable C container) {
        this.activeContainer = container;
    }

    public boolean isActiveContainer(C container) {
        return this.activeContainer == container;
    }

    @Override
    public int getMaxStackSize() {
        return LibUtils.getMaxStackSize(super.getMaxStackSize());
    }

    public void setItemNoUpdate(int index, ItemStack stack) {
        getItems().set(index, stack);
        if (!stack.isEmpty() && stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
    }

    @Override
    public void fromTag(ListTag tag) {
        for (int i = 0; i < this.getContainerSize(); i++) {
            setItemNoUpdate(i, ItemStack.EMPTY);
        }

        for (int k = 0; k < tag.size(); k++) {
            CompoundTag compoundtag = tag.getCompound(k);
            int j = compoundtag.getByte("Slot") & 255;
            if (j < getContainerSize()) {
                setItemNoUpdate(j, ItemStack.of(compoundtag));
            }
        }

        setChanged();
    }

    @Override
    public ListTag createTag() {
        ListTag listtag = new ListTag();

        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack itemstack = getItem(i);
            if (!itemstack.isEmpty()) {
                CompoundTag compoundtag = new CompoundTag();
                compoundtag.putByte("Slot", (byte) i);
                listtag.add(itemstack.save(compoundtag));
            }
        }

        return listtag;
    }

    @Override
    public boolean stillValid(Player player) {
        return (activeContainer == null || activeContainer.stillValid(player)) && super.stillValid(player);
    }

    @Override
    public void stopOpen(Player player) {
        super.stopOpen(player);
        this.activeContainer = null;
    }

    @Override
    public ListTag serializeNBT(HolderLookup.Provider provider) {
        return createTag();
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, ListTag nbt) {
        fromTag(nbt);
    }

    public interface ValidEntity {
        BlockEntity self();

        default boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(self(), player);
        }
    }
}
