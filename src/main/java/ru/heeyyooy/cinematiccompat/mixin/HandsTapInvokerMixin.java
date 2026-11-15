package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.EasyMagicRerollHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds a public bridge that performs exactly the operation of Hands.tap():
 * updates the timestamp consumed by Hands.follow() to make the working hand
 * press down briefly toward its current aim.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.Hands", remap = false)
public abstract class HandsTapInvokerMixin {
    @Shadow
    private static double tapAt;

    @Unique
    public static void cinematiccompat$tap() {
        tapAt = EasyMagicRerollHooks.clock();
    }
}
