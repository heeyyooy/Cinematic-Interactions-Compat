package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.EasyMagicRerollHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
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
    private Vec3 cinematiccompat$rerollPosition() {
        Vec3 lapis = this.corner();
        return new Vec3(
                this.top.x * 2.0 - lapis.x,
                lapis.y,
                this.top.z * 2.0 - lapis.z
        );
    }

    @Inject(method = "updateCursor", at = @At("HEAD"), remap = false)
    private void cinematiccompat$resetRerollHover(boolean overPanel, CallbackInfo ci) {
        this.cinematiccompat$hoveringReroll = false;
    }

    @Inject(method = "updateCursor", at = @At("RETURN"), remap = false)
    private void cinematiccompat$pickRerollButton(boolean overPanel, CallbackInfo ci) {
        if (overPanel
                || !EasyMagicRerollHooks.isButtonVisible()
                || !EasyMagicRerollHooks.hasAim(this)) {
            return;
        }

        Vec3 button = this.cinematiccompat$rerollPosition();
        if (EasyMagicRerollHooks.isCursorOver(button, 0.14)) {
            this.cinematiccompat$hoveringReroll = true;
            EasyMagicRerollHooks.setAim(this, button);
            this.aimAir = true;
        }
    }

    @Inject(method = "click", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$clickRerollButton(int button, boolean shift, CallbackInfo ci) {
        if (!this.cinematiccompat$hoveringReroll) {
            return;
        }

        ci.cancel();
        if (button != 0) {
            return;
        }

        if (EasyMagicRerollHooks.clickReroll()) {
            this.cinematiccompat$rerollFlashAt = EasyMagicRerollHooks.clock();
            EasyMagicRerollHooks.playRerollBurst(this.cinematiccompat$rerollPosition());
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.85f, 1.15f);
            }
        } else {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.playSound(SoundEvents.CHAIN_HIT, 0.45f, 0.85f);
            }
        }
    }

    @Inject(method = "hint", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$showRerollHint(CallbackInfoReturnable<Component> cir) {
        if (!this.cinematiccompat$hoveringReroll) {
            return;
        }

        cir.setReturnValue(Component.translatable(
                EasyMagicRerollHooks.canReroll()
                        ? "cinematiccompat.hint.reroll"
                        : "cinematiccompat.hint.reroll_unavailable"));
    }

    @Inject(method = "renderScene", at = @At("TAIL"), remap = false)
    private void cinematiccompat$renderRerollButton(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            int light,
            CallbackInfo ci) {
        if (!EasyMagicRerollHooks.isButtonVisible()) {
            return;
        }

        double flashAge = EasyMagicRerollHooks.clock() - this.cinematiccompat$rerollFlashAt;
        EasyMagicRerollHooks.renderButton(
                pose,
                buffers,
                camera,
                this.cinematiccompat$rerollPosition(),
                this.cinematiccompat$hoveringReroll,
                EasyMagicRerollHooks.canReroll(),
                flashAge,
                light
        );
    }
}
