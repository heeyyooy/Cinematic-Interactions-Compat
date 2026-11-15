package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets Amendments resolve valid modded LecternBlockEntity subclasses such as Carved Wood's. */
@Pseudo
@Mixin(targets = "net.mehvahdjukaar.amendments.common.LecternEditMenu", remap = false)
public abstract class AmendmentsLecternEntityMixin {
    @Redirect(
            method = "of",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/mehvahdjukaar/moonlight/api/misc/TileOrEntityTarget;getBlockEntityOrThrow(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/entity/BlockEntityType;)Lnet/minecraft/world/level/block/entity/BlockEntity;",
                    remap = false
            ),
            remap = false
    )
    private static BlockEntity cinematiccompat$acceptLecternSubtypes(
            @Coerce Object target, Level level, BlockEntityType<?> ignoredType) {
        try {
            BlockPos pos = (BlockPos) target.getClass().getMethod("getPos").invoke(target);
            BlockEntity entity = pos == null ? null : level.getBlockEntity(pos);
            if (entity instanceof LecternBlockEntity) {
                return entity;
            }
            throw new IllegalStateException("No lectern BlockEntity found at " + pos);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not resolve Amendments lectern target", exception);
        }
    }
}
