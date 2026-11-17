package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Reuses Cinematic Interactions' central 3D item pile in every state. During
 * the scene its own renderer is already active; afterwards this renderer keeps
 * the same orientation and position rather than reverting to Easy Anvils'
 * side-by-side flat layout.
 */
@Pseudo
@Mixin(targets = "fuzs.easyanvils.client.renderer.blockentity.AnvilRenderer", remap = false)
public abstract class EasyAnvilsRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$keepCinematicAnvilLayout(
            BlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            CallbackInfo ci) {
        if (blockEntity == null || !CinematicCompatClientHooks.easyAnvilsRenderContentsEnabled()) {
            return;
        }

        // During the scene Cinematic Interactions draws the same 3D pile.
        if (CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())) {
            ci.cancel();
            return;
        }

        if (blockEntity instanceof Container container) {
            Direction side = blockEntity.getBlockState().getValue(AnvilBlock.FACING);
            boolean rendered = CinematicCompatClientHooks.drawAnvilContentsAtBlockOrigin(
                    poseStack,
                    bufferSource,
                    side,
                    List.of(container.getItem(0), container.getItem(1)),
                    packedLight
            );
            if (rendered) {
                ci.cancel();
            }
        }
    }
}
