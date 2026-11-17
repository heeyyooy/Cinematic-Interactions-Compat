package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;
import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

/**
 * Suppresses Easy Magic's additional chiseled-bookshelf tooltip while the
 * matching carved bookshelf is being handled by Cinematic Interactions.
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
        if (minecraft.level == null || !(minecraft.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        BlockPos pos = hitResult.getBlockPos();
        if (CarvedBookshelfCompatHooks.isSupportedShelf(minecraft.level.getBlockState(pos))
                && CinematicCompatClientHooks.isSceneActiveAt(pos)) {
            ci.cancel();
        }
    }
}
