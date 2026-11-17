package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntity;

import java.util.List;

/**
 * Renders the persistent Dark Anvil inputs while the station is idle.
 * Prefer Cinematic Interactions' exact shared pile renderer; keep a local
 * equivalent as a fallback so the items remain visible if that bridge changes.
 */
public final class GoetyDarkAnvilRenderer implements BlockEntityRenderer<GoetyDarkAnvilBlockEntity> {
    public GoetyDarkAnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public boolean shouldRenderOffScreen(GoetyDarkAnvilBlockEntity blockEntity) {
        // The item pile sits above the block's model and can extend beyond its normal render bounds.
        return true;
    }

    @Override
    public void render(GoetyDarkAnvilBlockEntity blockEntity, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (CinematicCompatClientHooks.isSceneActiveAt(blockEntity.getBlockPos())) return;

        Direction side = blockEntity.getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        ItemStack main = blockEntity.getItem(0);
        ItemStack addition = blockEntity.getItem(1);

        boolean rendered = CinematicCompatClientHooks.drawAnvilContentsAtBlockOrigin(
                poseStack, bufferSource, side, List.of(main, addition), packedLight
        );
        if (!rendered) {
            renderFallbackPile(poseStack, bufferSource, side, main, addition, packedLight);
        }
    }

    /**
     * Default AnvilStation pile layout, matching Easy Anvils' normal two-item
     * arrangement when the shared Cinematic Interactions renderer is unavailable.
     */
    private static void renderFallbackPile(PoseStack pose, MultiBufferSource buffers, Direction side,
                                           ItemStack main, ItemStack addition, int light) {
        Vec3 top = new Vec3(0.5, 1.0, 0.5);
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getNormal());
        Vec3 outward = Vec3.atLowerCornerOf(side.getNormal());

        Vec3 mainPoint = top.add(right.scale(-0.02));
        Vec3 additionPoint = top.add(right.scale(0.07)).add(outward.scale(-0.03)).add(0, 0.012, 0);
        float yaw = 180.0f - side.getOpposite().toYRot() + 45.0f;

        if (!main.isEmpty()) {
            boolean block = isBlock(main);
            float size = block ? 0.2f : 0.4f;
            Vec3 at = mainPoint.add(0, block ? size / 2.0 : 0.004, 0);
            drawOriented(pose, buffers, main, at, yaw, size, light);
        }

        if (!addition.isEmpty()) {
            boolean block = isBlock(addition);
            float size = block ? 0.16f : 0.3f;
            Vec3 at = additionPoint.add(0, block ? size / 2.0 : -0.004, 0);
            drawOriented(pose, buffers, addition, at, yaw + 70.0f, size, light);

            if (addition.getCount() > 1) {
                drawOriented(pose, buffers, addition,
                        at.add(right.scale(0.03)).add(0, 0.008, 0), yaw + 95.0f, size, light);
            }
        }
    }

    private static boolean isBlock(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        return mc.getItemRenderer().getModel(stack, mc.level, null, 0).isGui3d();
    }

    private static void drawOriented(PoseStack pose, MultiBufferSource buffers, ItemStack stack,
                                     Vec3 center, float yaw, float size, int light) {
        if (stack.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        ItemRenderer renderer = mc.getItemRenderer();
        BakedModel model = renderer.getModel(stack, mc.level, null, 0);

        pose.pushPose();
        pose.translate(center.x, center.y, center.z);
        pose.mulPose(Axis.YP.rotationDegrees(yaw).mul(Axis.XP.rotationDegrees(-90.0f)));
        float modelScale = model.isGui3d() ? size * 2.0f : size;
        pose.scale(modelScale, modelScale, modelScale);
        renderer.render(stack, ItemDisplayContext.FIXED, false, pose, buffers, light, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();
    }
}
