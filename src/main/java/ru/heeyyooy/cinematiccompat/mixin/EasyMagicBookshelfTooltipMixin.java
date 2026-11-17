package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;
import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

/**
 * Suppresses Easy Magic's additional chiseled-bookshelf tooltip for the
 * entire duration of a Cinematic Interactions bookshelf scene.
 */
@Pseudo
@Mixin(targets = "fuzs.easymagic.client.handler.ChiseledBookshelfTooltipHandler", remap = false)
public abstract class EasyMagicBookshelfTooltipMixin {
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
