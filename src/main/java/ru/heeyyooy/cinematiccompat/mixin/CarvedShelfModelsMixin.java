package ru.heeyyooy.cinematiccompat.mixin;

import net.neoforged.neoforge.client.event.ModelEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;

/** Adds the same animated-empty-slot model wrapper to Carved Wood and Every Compat shelves. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.ShelfBooks", remap = false)
public abstract class CarvedShelfModelsMixin {
    @Inject(method = "emptySlots", at = @At("TAIL"), remap = false)
    private static void cinematiccompat$wrapCarvedShelfModels(
            ModelEvent.ModifyBakingResult event, CallbackInfo ci) {
        CarvedBookshelfCompatHooks.wrapModels(event);
    }
}