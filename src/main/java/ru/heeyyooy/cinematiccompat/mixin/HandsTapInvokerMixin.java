package ru.heeyyooy.cinematiccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes Cinematic Interactions' normal short hand-tap animation so the
 * physical Easy Magic reroll token can use the same hand response as station clicks.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.Hands", remap = false)
public interface HandsTapInvokerMixin {
    @Invoker(value = "tap", remap = false)
    static void cinematiccompat$tap() {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
