package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import ru.heeyyooy.cinematiccompat.client.CarvedBookshelfCompatHooks;

/** Resolves namespaced Carved Wood textures without Fx.sprite's minecraft default namespace. */
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
}