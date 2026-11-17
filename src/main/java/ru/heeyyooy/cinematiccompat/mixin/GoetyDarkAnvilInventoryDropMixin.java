package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;

/**
 * Drops stored items if a Goety dark anvil is actually removed.
 * Switching between normal/chipped/damaged variants keeps the same inventory.
 */
@Mixin(Block.class)
public abstract class GoetyDarkAnvilInventoryDropMixin {
    @Inject(method = "onRemove", at = @At("HEAD"))
    private void cinematiccompat$dropInventoryWhenDarkAnvilIsRemoved(
            BlockState oldState,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving,
            CallbackInfo ci
    ) {
        if (level.isClientSide || !cinematiccompat$isGoetyDarkAnvil(oldState)) {
            return;
        }
        if (cinematiccompat$isGoetyDarkAnvil(newState)) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof GoetyDarkAnvilBlockEntity inventory && !inventory.isEmpty()) {
            Containers.dropContents(level, pos, inventory);
        }
    }

    private static boolean cinematiccompat$isGoetyDarkAnvil(BlockState state) {
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (!"goety".equals(id.getNamespace())) {
            return false;
        }
        return id.getPath().equals("dark_anvil")
                || id.getPath().equals("chipped_dark_anvil")
                || id.getPath().equals("damaged_dark_anvil");
    }
}
