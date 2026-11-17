package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;
import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import java.lang.reflect.Method;
import java.util.OptionalInt;

/**
 * Suppresses Easy Magic's additional chiseled-bookshelf tooltip for the
 * entire duration of a Cinematic Interactions bookshelf scene.
 */
@Pseudo
@Mixin(targets = "fuzs.easymagic.client.handler.ChiseledBookshelfTooltipHandler", remap = false)
public abstract class EasyMagicBookshelfTooltipMixin {
    /**
     * Easy Magic assumes all shelves use the vanilla 3x2 slot grid. Carved Wood
     * shelves have individual book positions, so resolve those with the
     * compatibility geometry before falling back to Easy Magic's own method.
     */
    @Redirect(
            method = "onRenderGui",
            at = @At(
                    value = "INVOKE",
                    target = "Lfuzs/easymagic/mixin/client/accessor/ChiseledBookShelfBlockAccessor;easymagic$callGetHitSlot(Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/level/block/state/BlockState;)Ljava/util/OptionalInt;"
            ),
            remap = false
    )
    private static OptionalInt cinematiccompat$useCarvedShelfBookPositions(
            @Coerce Object accessor,
            BlockHitResult hitResult,
            BlockState blockState) {
        if (blockState.hasProperty(HorizontalDirectionalBlock.FACING)) {
            Direction facing = blockState.getValue(HorizontalDirectionalBlock.FACING);
            int slot = CarvedBookshelfCompatHooks.tooltipSlotAt(
                    hitResult.getBlockPos(), facing, hitResult.getLocation());
            if (slot != CarvedBookshelfCompatHooks.NOT_HANDLED) {
                return slot >= 0 ? OptionalInt.of(slot) : OptionalInt.empty();
            }
        }

        // Preserve Easy Magic's native hit testing for vanilla and other
        // bookshelf implementations which do not have custom slot metadata.
        try {
            Method method = accessor.getClass().getMethod(
                    "easymagic$callGetHitSlot", BlockHitResult.class, BlockState.class);
            Object result = method.invoke(accessor, hitResult, blockState);
            if (result instanceof OptionalInt optionalInt) {
                return optionalInt;
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // No matching accessor: do not render a tooltip for an unknown slot.
        }
        return OptionalInt.empty();
    }

    @Inject(method = "onRenderGui", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$hideTooltipDuringShelfScene(
            Minecraft minecraft,
            GuiGraphics guiGraphics,
            DeltaTracker deltaTracker,
            CallbackInfo ci) {
        if (minecraft.level == null) {
            return;
        }

        // The camera moves away from the shelf during the interaction, so the
        // crosshair hit result no longer identifies the active station.
        // Resolve the shelf from the scene itself instead of the current aim.
        BlockPos scenePos = CinematicCompatClientHooks.currentScenePos();
        if (scenePos != null
                && CinematicCompatClientHooks.isSceneActiveAt(scenePos)
                && CarvedBookshelfCompatHooks.isSupportedShelf(minecraft.level.getBlockState(scenePos))) {
            ci.cancel();
        }
    }
}
