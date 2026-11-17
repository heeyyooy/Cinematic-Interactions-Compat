package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;

/*
 * Portions of this renderer's item placement logic are adapted from
 * Fuzs / Easy Anvils (1.21.1), licensed under the Mozilla Public License 2.0.
 * Source: https://github.com/Fuzss/easy-anvils
 * See LICENSE-MPL-2.0.txt in the repository root.
 */

/**
 * Uses Easy Anvils' exact flat, side-by-side item layout for Goety's Dark Anvil.
 * Both inputs are independently placed according to the anvil's facing direction.
 */
public final class GoetyDarkAnvilRenderer implements BlockEntityRenderer<GoetyDarkAnvilBlockEntity> {
    private final ItemRenderer itemRenderer;

    public GoetyDarkAnvilRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRenderOffScreen(GoetyDarkAnvilBlockEntity blockEntity) {
        return true;
    }

    @Override
    public void render(GoetyDarkAnvilBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())
                || !CinematicCompatClientHooks.easyAnvilsRenderContentsEnabled()) {
            return;
        }

        Direction direction = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        int posData = (int) blockEntity.getBlockPos().asLong();
        Level level = blockEntity.getLevel();

        this.renderFlatItem(0, blockEntity.getItem(0), direction, poseStack, bufferSource,
                packedLight, packedOverlay, posData, level);
        this.renderFlatItem(1, blockEntity.getItem(1), direction, poseStack, bufferSource,
                packedLight, packedOverlay, posData, level);
    }

    /**
     * This transform is copied from Easy Anvils 1.21.1 AnvilRenderer:
     * flat items are placed in separate left/right positions and mirrored
     * based on the block facing, instead of being stacked at the center.
     */
    private void renderFlatItem(int index, ItemStack stack, Direction direction, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay,
                                int posData, Level level) {
        if (stack.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.0, 1.0375, 0.0);
        poseStack.mulPose(Axis.XN.rotationDegrees(90.0F));

        boolean mirrored = (direction.getAxisDirection().getStep() == 1 ? 1 : 0) != index % 2;
        switch (direction.getAxis()) {
            case X -> {
                if (mirrored) {
                    poseStack.translate(0.25, -0.5, 0.0);
                } else {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
                    poseStack.translate(-0.75, 0.5, 0.0);
                }
            }
            case Z -> {
                if (mirrored) {
                    poseStack.mulPose(Axis.ZN.rotationDegrees(90.0F));
                    poseStack.translate(0.25, 0.5, 0.0);
                } else {
                    poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    poseStack.translate(-0.75, -0.5, 0.0);
                }
            }
        }

        poseStack.scale(0.375F, 0.375F, 0.375F);
        this.itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, packedLight, packedOverlay,
                poseStack, bufferSource, level, posData + index);
        poseStack.popPose();
    }
}
