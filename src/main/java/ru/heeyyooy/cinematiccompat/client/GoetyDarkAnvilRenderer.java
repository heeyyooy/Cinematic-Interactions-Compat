package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;

import java.util.List;

/** Uses the same central item pile renderer as the Easy Anvils integration. */
public final class GoetyDarkAnvilRenderer implements BlockEntityRenderer<GoetyDarkAnvilBlockEntity> {
    public GoetyDarkAnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(GoetyDarkAnvilBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())) return;
        Direction side = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        CinematicCompatClientHooks.drawAnvilContentsAtBlockOrigin(
                poseStack, bufferSource, side,
                List.of(blockEntity.getItem(0), blockEntity.getItem(1)), packedLight
        );
    }
}
