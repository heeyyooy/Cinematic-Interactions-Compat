package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;

/** Resolves namespaced shelf textures and rotates UVs for horizontal Carved Wood books. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.OpenBook", remap = false)
public abstract class CarvedOpenBookTextureMixin {
    @Redirect(
            method = {"drawCover", "drawCoverFace", "drawShut"},
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/Fx;sprite(Ljava/lang/String;)Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;"
            ),
            remap = false
    )
    private TextureAtlasSprite cinematiccompat$resolveNamespacedBookTexture(String texture) {
        return CarvedBookshelfCompatHooks.sprite(texture);
    }

    @Inject(method = "boardUv", at = @At("RETURN"), cancellable = true, remap = false)
    private void cinematiccompat$rotateHorizontalBookBoard(
            TextureAtlasSprite sprite, boolean mirrored, CallbackInfoReturnable<float[]> cir) {
        cir.setReturnValue(CarvedBookshelfCompatHooks.rotateUvs(cir.getReturnValue()));
    }

    /**
     * OpenBook.drawShut() builds the spine UVs independently of boardUv().
     * Rotate that first face() call as well, otherwise the broad board is
     * mapped correctly but the complete horizontal spine image is stretched
     * along the book's vertical side.
     */
    @ModifyArg(
            method = "drawShut",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/dyslexic/cinematic/client/OpenBook;face(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;[FLnet/minecraft/world/phys/Vec3;IF)V",
                    ordinal = 0
            ),
            index = 7,
            remap = false
    )
    private float[] cinematiccompat$rotateHorizontalSpine(float[] uv) {
        return CarvedBookshelfCompatHooks.rotateUvs(uv);
    }
}