package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents Easy Anvils from drawing its stored items over Cinematic
 * Interactions' anvil scene. Its renderer resumes unchanged when the scene ends.
 */
@Pseudo
@Mixin(targets = "fuzs.easyanvils.client.renderer.blockentity.AnvilRenderer", remap = false)
public abstract class EasyAnvilsRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$hideDuplicateAnvilItems(
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci) {
        if (blockEntity != null
                && CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())) {
            ci.cancel();
        }
    }
}
