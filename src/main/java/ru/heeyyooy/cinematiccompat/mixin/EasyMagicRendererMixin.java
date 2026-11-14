package ru.heeyyooy.cinematiccompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Hides Easy Magic's second copy of the table contents while Cinematic
 * Interactions draws its own scene items. The vanilla enchanting-book animation
 * remains visible, and the block entity inventory is never modified.
 */
@Pseudo
@Mixin(targets = "fuzs.easymagic.client.renderer.blockentity.ModEnchantTableRenderer", remap = false)
public abstract class EasyMagicRendererMixin {
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
    private boolean cinematiccompat$shouldHideItems() {
        return this.cinematiccompat$renderingPos != null
                && CinematicCompatClientHooks.isSceneActiveAt(this.cinematiccompat$renderingPos);
    }

    @Inject(method = "renderFlatItemList", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$hideFlatItems(
            List<ItemStack> items,
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
    private void cinematiccompat$hideFloatingItem(
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
    private void cinematiccompat$hideFloatingItems(
            List<ItemStack> items,
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
}
