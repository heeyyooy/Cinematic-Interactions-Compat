package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;

/**
 * Gives Goety's three DarkAnvilBlock variants a persistent two-slot block entity.
 * All three variants share one BlockEntityType so damage-stage changes keep their contents.
 */
@Pseudo
@Mixin(targets = "com.Polarice3.Goety.common.blocks.DarkAnvilBlock", remap = false)
public abstract class GoetyDarkAnvilBlockMixin implements EntityBlock {
    @Unique
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GoetyDarkAnvilBlockEntity(pos, state);
    }
}
