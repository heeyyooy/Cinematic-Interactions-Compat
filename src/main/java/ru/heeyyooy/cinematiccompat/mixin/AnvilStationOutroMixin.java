package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * During the anvil outro, keep drawing the saved input stacks with the same
 * stacked 3D renderer used during the active scene. Do not switch to the
 * generic MenuStation.drawLeaving animation, which separates the inputs.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.AnvilStation", remap = false)
public abstract class AnvilStationOutroMixin {
    @Shadow
    protected abstract List<ItemStack> leftBehind();

    @Inject(method = "renderScene", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$keepAnvilPileDuringOutro(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            int light,
            CallbackInfo ci) {
        if (CinematicCompatClientHooks.isAnvilOutro()
                && CinematicCompatClientHooks.drawCurrentAnvilContents(
                        pose,
                        buffers,
                        camera.getPosition(),
                        this.leftBehind(),
                        light)) {
            ci.cancel();
        }
    }
}
