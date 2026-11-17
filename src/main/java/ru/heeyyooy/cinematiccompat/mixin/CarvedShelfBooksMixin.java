package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;

/** Replaces vanilla shelf book placement and spine UV selection for Carved Wood shelves. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.ShelfBooks", remap = false)
public abstract class CarvedShelfBooksMixin {
    @Inject(method = "spine", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$carvedSpine(
            BlockPos pos, Direction facing, int slot, CallbackInfoReturnable<Vec3> cir) {
        Vec3 point = CarvedBookshelfCompatHooks.spine(pos, facing, slot);
        if (point != null) {
            cir.setReturnValue(point);
        }
    }

    @Inject(method = "inSlot", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$carvedBookPose(
            BlockPos pos, Direction facing, int slot, double out, CallbackInfoReturnable<Object> cir) {
        Object pose = CarvedBookshelfCompatHooks.inSlot(pos, facing, slot, out);
        if (pose != null) {
            cir.setReturnValue(pose);
        }
    }

    @Inject(method = "look", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$carvedBookLook(int slot, CallbackInfoReturnable<Object> cir) {
        Object look = CarvedBookshelfCompatHooks.look(slot);
        if (look != null) {
            cir.setReturnValue(look);
        }
    }

    @Redirect(
            method = {"cue", "renderAll"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"),
            remap = false
    )
    private static boolean cinematiccompat$recognizeCarvedBookshelves(BlockState state, Block expected) {
        return CarvedBookshelfCompatHooks.shelfCheck(state, expected);
    }
}