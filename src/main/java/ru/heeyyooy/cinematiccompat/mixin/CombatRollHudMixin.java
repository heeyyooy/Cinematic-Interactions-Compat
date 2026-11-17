package ru.heeyyooy.cinematiccompat.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

/**
 * Combat Roll renders its cooldown widget from a separate NeoForge RenderGuiEvent listener.
 * Cancel only that listener while a Cinematic Interactions station scene is active.
 */
@Pseudo
@Mixin(targets = "net.combat_roll.neoforge.client.ClientEventsNeoForge", remap = false)
public abstract class CombatRollHudMixin {
    @Inject(method = "onRenderHud", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinematiccompat$hideCombatRollHudDuringScene(
            @Coerce Object event,
            CallbackInfo ci
    ) {
        if (CinematicCompatClientHooks.isSceneActive()) {
            ci.cancel();
        }
    }
}
