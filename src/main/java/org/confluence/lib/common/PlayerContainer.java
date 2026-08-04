package org.confluence.lib.common;

import net.minecraft.world.Container;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;

import javax.annotation.Nullable;

public class PlayerContainer<C extends BlockEntity & PlayerContainer.ValidEntity> extends SimpleContainer implements ValueIOSerializable {
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

    public void setItemNoUpdate(int index, ItemStack stack) {
        setItem(index, stack, true);
    }

    @Override
    public boolean stillValid(Player player) {
        return (activeContainer == null || activeContainer.stillValid(player)) && super.stillValid(player);
    }

    @Override
    public void serialize(ValueOutput output) {
        ValueOutput.TypedOutputList<ItemStackWithSlot> itemList = output.list("Items", ItemStackWithSlot.CODEC);
        for (int i = 0; i < getItems().size(); i++) {
            var stack = getItems().get(i);
            if (!stack.isEmpty()) {
                itemList.add(new ItemStackWithSlot(i, stack));
            }
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        input.listOrEmpty("Items", ItemStackWithSlot.CODEC).forEach(slot -> {
            if (slot.isValidInContainer(getContainerSize())) {
                setItemNoUpdate(slot.slot(), slot.stack());
            }
        });
    }

    public interface ValidEntity {
        BlockEntity self();

        default boolean stillValid(Player player) {
            return Container.stillValidBlockEntity(self(), player);
        }
    }
}
