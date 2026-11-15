package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * MenuStation normally animates every leftover input flying into the player's
 * inventory during an outro. Easy Anvils keeps its input stacks in the anvil,
 * so leave the interpolation at zero for anvil scenes: the same items stay
 * drawn at their rest points until the cinematic scene ends.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.MenuStation", remap = false)
public abstract class MenuStationAnvilOutroMixin {
    @Redirect(
            method = "drawLeaving",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/Anim;window(DDD)D"
            ),
            remap = false
    )
    private double cinematiccompat$keepAnvilInputsOnBlock(double time, double start, double end) {
        if (CinematicCompatClientHooks.isAnvilOutro()) {
            // Anim.inOutCubic(0) keeps each stored input exactly at restPoint().
            return 0.0;
        }

        if (end <= start) {
            return time >= end ? 1.0 : 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (time - start) / (end - start)));
    }
}
