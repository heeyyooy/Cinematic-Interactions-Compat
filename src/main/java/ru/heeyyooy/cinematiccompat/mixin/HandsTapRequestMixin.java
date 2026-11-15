package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.EasyMagicRerollHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies a queued reroll-button press inside the normal hand-follow update.
 * This updates the exact timestamp used by Hands.tap(), without adding a
 * public/static helper method to the target Mixin class.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.Hands", remap = false)
public abstract class HandsTapRequestMixin {
    @Shadow
    private static double tapAt;

    @Inject(method = "follow", at = @At("HEAD"), remap = false)
    private static void cinematiccompat$applyRequestedTap(CallbackInfo ci) {
        if (EasyMagicRerollHooks.consumeHandTapRequest()) {
            tapAt = EasyMagicRerollHooks.clock();
        }
    }
}
