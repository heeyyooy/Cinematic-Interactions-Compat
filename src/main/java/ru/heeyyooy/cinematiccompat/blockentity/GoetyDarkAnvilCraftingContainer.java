package ru.heeyyooy.cinematiccompat.blockentity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Container adapter that keeps Goety's normal AnvilMenu logic but stores inputs on the block. */
public final class GoetyDarkAnvilCraftingContainer extends SimpleContainer {
    private final GoetyDarkAnvilBlockEntity inventory;

    public GoetyDarkAnvilCraftingContainer(GoetyDarkAnvilBlockEntity inventory) {
        super(GoetyDarkAnvilBlockEntity.SLOT_COUNT);
        this.inventory = inventory;
    }

    @Override public int getContainerSize() { return this.inventory.getContainerSize(); }
    @Override public boolean isEmpty() { return this.inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return this.inventory.getItem(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { return this.inventory.removeItem(slot, amount); }
    @Override public ItemStack removeItemNoUpdate(int slot) { return this.inventory.removeItemNoUpdate(slot); }
    @Override public void setItem(int slot, ItemStack stack) { this.inventory.setItem(slot, stack); }
    @Override public int getMaxStackSize() { return this.inventory.getMaxStackSize(); }
    @Override public void setChanged() { this.inventory.setChanged(); }
    @Override public boolean stillValid(Player player) { return this.inventory.stillValid(player); }
    @Override public void clearContent() { this.inventory.clearContent(); }
    @Override public NonNullList<ItemStack> getItems() { return this.inventory.getItems(); }
}
