package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;

/** Uses the real Carved Wood slot rectangles for cursor and hover geometry. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.ShelfStation", remap = false)
public abstract class CarvedShelfStationMixin {
    @Inject(method = "slotAt", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$useCarvedSlotLayout(
            BlockPos pos, Direction facing, Vec3 point, CallbackInfoReturnable<Integer> cir) {
        int slot = CarvedBookshelfCompatHooks.slotAt(pos, facing, point);
        if (slot != CarvedBookshelfCompatHooks.NOT_HANDLED) {
            cir.setReturnValue(slot);
        }
    }

    @Redirect(
            method = "renderWorld",
            at = @At(value = "FIELD", target = "Ldev/dyslexic/cinematic/client/ShelfBooks;SPINES:[[I", opcode = 178),
            remap = false
    )
    private int[][] cinematiccompat$useCarvedHoverRectangles() {
        return CarvedBookshelfCompatHooks.spinesForCurrentScene();
    }
}