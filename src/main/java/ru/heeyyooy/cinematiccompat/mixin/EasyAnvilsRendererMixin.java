package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
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
 * Easy Anvils normally separates the two inputs on its anvil renderer. Use
 * Cinematic Interactions' own stacked 3D renderer instead, so the item layout
 * does not jump sideways when the cinematic scene ends.
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
        if (blockEntity == null) {
            return;
        }

        // During the scene, Cinematic Interactions already draws the same
        // 3D pile; don't let Easy Anvils draw a second copy.
        if (CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())) {
            ci.cancel();
            return;
        }

        // Once the scene ends, keep using the exact same 3D stacked layout
        // rather than switching to Easy Anvils' two side-by-side flat items.
        if (blockEntity instanceof Container container) {
            Direction side = blockEntity.getBlockState().getValue(AnvilBlock.FACING);
            boolean rendered = CinematicCompatClientHooks.drawAnvilContents(
                    poseStack,
                    bufferSource,
                    net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().getPosition(),
                    blockEntity.getBlockPos(),
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
