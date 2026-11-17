package ru.heeyyooy.cinematiccompat.blockentity;

import net.minecraft.core.NonNullList;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * Adapts a Goety dark anvil's persistent block inventory to the normal AnvilMenu.
 * Changes are forwarded both to the block entity (world/save synchronization)
 * and to the menu (anvil result recalculation).
 */
public final class GoetyDarkAnvilCraftingContainer extends SimpleContainer {
    private final AbstractContainerMenu menu;
    private final GoetyDarkAnvilBlockEntity inventory;

    public GoetyDarkAnvilCraftingContainer(AbstractContainerMenu menu, GoetyDarkAnvilBlockEntity inventory) {
        super(GoetyDarkAnvilBlockEntity.SLOT_COUNT);
        this.menu = menu;
        this.inventory = inventory;
    }

    @Override public int getContainerSize() { return this.inventory.getContainerSize(); }
    @Override public boolean isEmpty() { return this.inventory.isEmpty(); }
    @Override public ItemStack getItem(int slot) { return this.inventory.getItem(slot); }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack stack = this.inventory.removeItem(slot, amount);
        if (!stack.isEmpty()) this.menu.slotsChanged(this);
        return stack;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = this.inventory.removeItemNoUpdate(slot);
        if (!stack.isEmpty()) {
            this.inventory.setChanged();
            this.menu.slotsChanged(this);
        }
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        this.inventory.setItem(slot, stack);
        this.menu.slotsChanged(this);
    }

    @Override public int getMaxStackSize() { return this.inventory.getMaxStackSize(); }

    @Override
    public void setChanged() {
        this.inventory.setChanged();
        this.menu.slotsChanged(this);
    }

    @Override public boolean stillValid(Player player) { return this.inventory.stillValid(player); }

    @Override
    public void clearContent() {
        this.inventory.clearContent();
        this.menu.slotsChanged(this);
    }

    @Override public NonNullList<ItemStack> getItems() { return this.inventory.getItems(); }
}
