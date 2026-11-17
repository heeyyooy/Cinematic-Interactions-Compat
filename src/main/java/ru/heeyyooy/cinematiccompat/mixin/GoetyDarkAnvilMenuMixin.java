package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilCraftingContainer;

@Pseudo
@Mixin(targets = "com.Polarice3.Goety.client.inventory.container.DarkAnvilMenu", remap = false)
public abstract class GoetyDarkAnvilMenuMixin {
    @Inject(
            method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void cinematiccompat$usePersistentBlockInventory(
            int containerId,
            Inventory inventory,
            ContainerLevelAccess access,
            CallbackInfo ci
    ) {
        access.execute((level, pos) -> {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof GoetyDarkAnvilBlockEntity darkAnvilInventory)) {
                return;
            }

            GoetyDarkAnvilCraftingContainer persistent =
                    new GoetyDarkAnvilCraftingContainer((AbstractContainerMenu) (Object) this, darkAnvilInventory);

            ItemCombinerMenuAccessor menuAccessor = (ItemCombinerMenuAccessor) this;
            menuAccessor.cinematiccompat$setInputSlots(persistent);

            AbstractContainerMenu menu = (AbstractContainerMenu) (Object) this;
            ((SlotContainerAccessor) (Object) menu.getSlot(0)).cinematiccompat$setContainer(persistent);
            ((SlotContainerAccessor) (Object) menu.getSlot(1)).cinematiccompat$setContainer(persistent);
        });
    }
}
