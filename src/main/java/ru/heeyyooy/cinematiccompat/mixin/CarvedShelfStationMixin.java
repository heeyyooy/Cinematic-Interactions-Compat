package ru.heeyyooy.cinematiccompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;
import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

/** Uses Carved Wood slot rectangles and keeps each book's texture bound to its shelf while rendering. */
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

    @Inject(method = "renderBooks", at = @At("HEAD"), remap = false)
    private void cinematiccompat$beginShelfTextureScope(
            PoseStack pose, MultiBufferSource buffers, Camera camera, ClientLevel level, CallbackInfo ci) {
        BlockPos pos = CinematicCompatClientHooks.currentScenePos();
        if (pos != null) {
            CarvedBookshelfCompatHooks.beginShelfRender(pos);
        }
    }

    @Inject(method = "renderBooks", at = @At("RETURN"), remap = false)
    private void cinematiccompat$endShelfTextureScope(
            PoseStack pose, MultiBufferSource buffers, Camera camera, ClientLevel level, CallbackInfo ci) {
        CarvedBookshelfCompatHooks.endShelfRender();
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