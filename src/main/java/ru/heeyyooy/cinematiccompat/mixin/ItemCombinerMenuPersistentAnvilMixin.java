package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * ItemCombinerMenu normally returns its input slots to the player when it closes.
 * For Goety's dark anvil, the slots are backed by the block, so skip only that
 * return step while preserving AbstractContainerMenu's normal close behavior.
 */
@Mixin(ItemCombinerMenu.class)
public abstract class ItemCombinerMenuPersistentAnvilMixin {
    @Unique
    private ContainerLevelAccess cinematiccompat$accessBeforeClose;

    @Inject(method = "removed", at = @At("HEAD"))
    private void cinematiccompat$keepDarkAnvilInputsOnBlock(Player player, CallbackInfo ci) {
        if (!this.getClass().getName().equals("com.Polarice3.Goety.client.inventory.container.DarkAnvilMenu")) {
            return;
        }

        ItemCombinerMenuAccessor accessor = (ItemCombinerMenuAccessor) this;
        this.cinematiccompat$accessBeforeClose = accessor.cinematiccompat$getAccess();
        accessor.cinematiccompat$setAccess(ContainerLevelAccess.NULL);
    }

    @Inject(method = "removed", at = @At("RETURN"))
    private void cinematiccompat$restoreDarkAnvilAccess(Player player, CallbackInfo ci) {
        if (this.cinematiccompat$accessBeforeClose == null) {
            return;
        }

        ((ItemCombinerMenuAccessor) this).cinematiccompat$setAccess(this.cinematiccompat$accessBeforeClose);
        this.cinematiccompat$accessBeforeClose = null;
    }
}
