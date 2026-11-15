package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Keeps the enchanted item visible on the table throughout the exit phase,
 * while the lapis pieces, their count and the lapis socket ease back in.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.EnchantStation", remap = false)
public abstract class EnchantStationOutroMixin {
    private static final double OUTRO_REVEAL_SECONDS = 0.45;

    /**
     * renderScene computes fade = 1 - smoother(Scene.time / 0.45) for its
     * outro. Make that fade stay at one; the lapis is faded independently
     * below, while the item itself remains at its table position.
     *
     * The third smoother call in renderScene is the outro fade. The earlier
     * calls belong to the intro glow and item rotation.
     */
    @Redirect(
            method = "renderScene",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/Anim;smoother(D)D",
                    ordinal = 2
            ),
            remap = false
    )
    private double cinematiccompat$keepEnchantingItemVisible(double value) {
        return CinematicCompatClientHooks.isSceneOutro()
                ? 0.0
                : cinematiccompat$smoother(value);
    }

    @ModifyArg(
            method = "renderScene",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/EnchantStation;drawLapis(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;IIFF)V"
            ),
            index = 6,
            remap = false
    )
    private float cinematiccompat$revealLapis(float scale) {
        return CinematicCompatClientHooks.isSceneOutro()
                ? scale * CinematicCompatClientHooks.outroProgress(OUTRO_REVEAL_SECONDS)
                : scale;
    }

    @ModifyArg(
            method = "renderScene",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/EnchantStation;drawLapisCount(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/Camera;Lnet/minecraft/world/phys/Vec3;IF)V"
            ),
            index = 5,
            remap = false
    )
    private float cinematiccompat$revealLapisCount(float presence) {
        return CinematicCompatClientHooks.isSceneOutro()
                ? presence * CinematicCompatClientHooks.outroProgress(OUTRO_REVEAL_SECONDS)
                : presence;
    }

    @ModifyArg(
            method = "renderScene",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/EnchantFx;circle(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;DDLdev/dyslexic/cinematic/client/EnchantFx$CirclePalette;F)V",
                    ordinal = 2
            ),
            index = 7,
            remap = false
    )
    private float cinematiccompat$revealLapisSocket(float brightness) {
        return CinematicCompatClientHooks.isSceneOutro()
                ? brightness * CinematicCompatClientHooks.outroProgress(OUTRO_REVEAL_SECONDS)
                : brightness;
    }

    private static double cinematiccompat$smoother(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }
}
