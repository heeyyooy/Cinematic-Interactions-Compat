package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.client.resources.model.BakedModel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hides Easy Magic's duplicate item rendering during the active scene, then
 * cross-fades its normal table items back in during the outro. The Cinematic
 * Interactions item and lapis fade out over the same interval.
 */
@Pseudo
@Mixin(targets = "fuzs.easymagic.client.renderer.blockentity.ModEnchantTableRenderer", remap = false)
public abstract class EasyMagicRendererMixin {
    private static final double CINEMATICCOMPAT$OUTRO_SECONDS = 0.45;

    @Unique
    private BlockPos cinematiccompat$renderingPos;

    @Inject(method = "render", at = @At("HEAD"), remap = false)
    private void cinematiccompat$captureRenderingPosition(
            EnchantingTableBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            int combinedOverlayIn,
            CallbackInfo ci) {
        this.cinematiccompat$renderingPos = blockEntity.getBlockPos();
    }

    @Inject(method = "render", at = @At("RETURN"), remap = false)
    private void cinematiccompat$clearRenderingPosition(
            EnchantingTableBlockEntity blockEntity,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            int combinedOverlayIn,
            CallbackInfo ci) {
        this.cinematiccompat$renderingPos = null;
    }

    @Unique
    private boolean cinematiccompat$isMatchingScene() {
        return this.cinematiccompat$renderingPos != null
                && CinematicCompatClientHooks.isSceneActiveAt(this.cinematiccompat$renderingPos);
    }

    @Unique
    private boolean cinematiccompat$shouldHideItems() {
        return this.cinematiccompat$isMatchingScene()
                && !CinematicCompatClientHooks.isSceneOutro();
    }

    @Unique
    private float cinematiccompat$outroScale() {
        return this.cinematiccompat$isMatchingScene()
                && CinematicCompatClientHooks.isSceneOutro()
                ? CinematicCompatClientHooks.outroProgress(CINEMATICCOMPAT$OUTRO_SECONDS)
                : 1.0f;
    }

    @Inject(method = "renderFlatItemList", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$hideFlatItemsDuringWork(
            java.util.List<ItemStack> items,
            BlockPos pos,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            int combinedOverlayIn,
            Level level,
            int posData,
            CallbackInfo ci) {
        if (this.cinematiccompat$shouldHideItems()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHoveringItem", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$hideFloatingItemDuringWork(
            EnchantingTableBlockEntity blockEntity,
            ItemStack itemToEnchant,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            CallbackInfo ci) {
        if (this.cinematiccompat$shouldHideItems()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHoveringItemList", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$hideFloatingItemsDuringWork(
            java.util.List<ItemStack> items,
            float age,
            PoseStack poseStack,
            MultiBufferSource bufferIn,
            int combinedLightIn,
            int combinedOverlayIn,
            boolean rotateItems,
            Level level,
            int posData,
            CallbackInfo ci) {
        if (this.cinematiccompat$shouldHideItems()) {
            ci.cancel();
        }
    }

    /**
     * Easy Magic's FLAT layout uses renderStatic for every item. Scaling the
     * incoming pose stack cross-fades the complete Easy Magic layout in.
     */
    @Redirect(
            method = "renderFlatItemList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"
            ),
            remap = false
    )
    private void cinematiccompat$fadeFlatItemsIn(
            ItemRenderer renderer,
            ItemStack stack,
            ItemDisplayContext displayContext,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Level level,
            int seed) {
        float scale = this.cinematiccompat$outroScale();
        if (scale <= 0.001f) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderer.renderStatic(stack, displayContext, packedLight, packedOverlay, poseStack, buffers, level, seed);
        poseStack.popPose();
    }

    /**
     * Easy Magic's FLOATING layout uses the same renderStatic overload for
     * the lapis and reroll-catalyst items orbiting the table.
     */
    @Redirect(
            method = "renderHoveringItemList",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"
            ),
            remap = false
    )
    private void cinematiccompat$fadeFloatingItemsIn(
            ItemRenderer renderer,
            ItemStack stack,
            ItemDisplayContext displayContext,
            int packedLight,
            int packedOverlay,
            PoseStack poseStack,
            MultiBufferSource buffers,
            Level level,
            int seed) {
        float scale = this.cinematiccompat$outroScale();
        if (scale <= 0.001f) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderer.renderStatic(stack, displayContext, packedLight, packedOverlay, poseStack, buffers, level, seed);
        poseStack.popPose();
    }

    /**
     * The enchanted input itself is rendered separately in FLOATING mode. It
     * cross-fades back in as Cinematic Interactions' copy fades off the table.
     */
    @Redirect(
            method = "renderHoveringItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"
            ),
            remap = false
    )
    private void cinematiccompat$fadeFloatingInputIn(
            ItemRenderer renderer,
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay,
            BakedModel model) {
        float scale = this.cinematiccompat$outroScale();
        if (scale <= 0.001f) {
            return;
        }

        poseStack.pushPose();
        poseStack.scale(scale, scale, scale);
        renderer.render(stack, displayContext, leftHand, poseStack, buffers, packedLight, packedOverlay, model);
        poseStack.popPose();
    }
}
