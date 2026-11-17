package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.EasyMagicRerollHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds Easy Magic's actual reroll action to the physical enchanting-table scene.
 * Its position mirrors the lapis stack on the opposite side of the book.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.EnchantStation", remap = false)
public abstract class EnchantStationRerollButtonMixin {
    @Shadow @Final public Vec3 top;
    @Shadow public abstract Vec3 corner();
    @Shadow private boolean aimAir;

    @Unique
    private boolean cinematiccompat$hoveringReroll;

    @Unique
    private double cinematiccompat$rerollFlashAt = -10.0;

    @Unique
    private boolean cinematiccompat$rerollVisibleAtExit;

    @Unique
    private boolean cinematiccompat$rerollAffordableAtExit;

    @Unique
    private Vec3 cinematiccompat$rerollPosition() {
        Direction side = EasyMagicRerollHooks.stationSide(this);
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getNormal());
        Vec3 offset = this.corner().subtract(this.top);
        // Mirror only the left/right component; preserve player distance and height.
        return this.top.add(offset).subtract(right.scale(2.0 * offset.dot(right)));
    }

    @Inject(method = "onEnd", at = @At("HEAD"), remap = false)
    private void cinematiccompat$rememberRerollForOutro(CallbackInfo ci) {
        // EnchantStation.onEnd switches the local player back to inventoryMenu.
        // Cache the button state before that happens so its exit animation can finish.
        this.cinematiccompat$rerollVisibleAtExit = EasyMagicRerollHooks.isButtonVisible();
        this.cinematiccompat$rerollAffordableAtExit = EasyMagicRerollHooks.canReroll();
    }

    @Inject(method = "updateCursor", at = @At("HEAD"), remap = false)
    private void cinematiccompat$resetRerollHover(boolean overPanel, CallbackInfo ci) {
        this.cinematiccompat$hoveringReroll = false;
    }

    @Inject(method = "updateCursor", at = @At("RETURN"), remap = false)
    private void cinematiccompat$pickRerollButton(boolean overPanel, CallbackInfo ci) {
        if (overPanel || !EasyMagicRerollHooks.isButtonVisible()) {
            return;
        }

        Vec3 button = this.cinematiccompat$rerollPosition();
        Direction side = EasyMagicRerollHooks.stationSide(this);
        Vec3 right = Vec3.atLowerCornerOf(side.getOpposite().getClockWise().getNormal());
        Vec3 clockCenter = EasyMagicRerollHooks.clockModelPosition(button, side);
        // Keep the wider hit target centred on the corrected visible clock position.
        Vec3 hitCenter = clockCenter.subtract(right.scale(0.035));
        if (EasyMagicRerollHooks.isCursorOver(hitCenter, 0.18)) {
            this.cinematiccompat$hoveringReroll = true;
            EasyMagicRerollHooks.setAim(this, clockCenter);
            this.aimAir = true;
        }
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$clickRerollButton(int button, boolean shift, CallbackInfo ci) {
        if (!this.cinematiccompat$hoveringReroll) {
            return;
        }

        // This surface is a button, not the lapis or item input slot.
        ci.cancel();
        if (button != 0) {
            return;
        }

        Vec3 buttonPosition = this.cinematiccompat$rerollPosition();
        Vec3 clockCenter = EasyMagicRerollHooks.clockModelPosition(
                buttonPosition,
                EasyMagicRerollHooks.stationSide(this));
        // Keep the working hand aimed at the visible centre of the token.
        EasyMagicRerollHooks.setAim(this, clockCenter);
        this.aimAir = true;
        EasyMagicRerollHooks.tapHand();

        if (EasyMagicRerollHooks.clickReroll()) {
            this.cinematiccompat$rerollFlashAt = EasyMagicRerollHooks.clock();
            EasyMagicRerollHooks.playRerollBurst(buttonPosition);
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.85f, 1.15f);
            }
        } else {
            EasyMagicRerollHooks.markRerollDenied();
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                // Same deny feedback as Cinematic Interactions' unaffordable enchant offer.
                player.playSound(SoundEvents.CANDLE_EXTINGUISH, 0.7f, 0.7f);
            }
        }
    }

    @Inject(method = "hint", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$showRerollHint(CallbackInfoReturnable<Component> cir) {
        if (!this.cinematiccompat$hoveringReroll) {
            return;
        }

        cir.setReturnValue(EasyMagicRerollHooks.rerollHint());
    }

    @Inject(method = "renderScene", at = @At("TAIL"), remap = false)
    private void cinematiccompat$renderRerollButton(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            int light,
            CallbackInfo ci) {
        boolean menuButtonVisible = EasyMagicRerollHooks.isButtonVisible();
        boolean fadingOut = EasyMagicRerollHooks.isSceneOutro() && this.cinematiccompat$rerollVisibleAtExit;
        if (!menuButtonVisible && !fadingOut) {
            return;
        }

        boolean affordable = menuButtonVisible
                ? EasyMagicRerollHooks.canReroll()
                : this.cinematiccompat$rerollAffordableAtExit;
        double flashAge = EasyMagicRerollHooks.clock() - this.cinematiccompat$rerollFlashAt;
        EasyMagicRerollHooks.renderButton(
                pose,
                buffers,
                camera,
                this.cinematiccompat$rerollPosition(),
                EasyMagicRerollHooks.stationSide(this),
                this.cinematiccompat$hoveringReroll,
                affordable,
                flashAge,
                light,
                this
        );
    }
}
