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
 * MenuStation normally animates leftover input slots as separate items flying
 * toward the camera. Anvil scenes instead keep drawing the exact same stacked
 * 3D pile as the active anvil scene.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.MenuStation", remap = false)
public abstract class MenuStationAnvilOutroMixin {
    @Shadow
    protected abstract List<ItemStack> leftBehind();

    @Inject(method = "drawLeaving", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$keepAnvilPileThroughOutro(
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
