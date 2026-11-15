package ru.heeyyooy.cinematiccompat.mixin;

import ru.heeyyooy.cinematiccompat.client.CinematicCompatClientHooks;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Replaces AnvilStation's actual outro renderer. MenuStation.drawLeaving()
 * animates each input towards the camera as if it were returned to inventory.
 * Easy Anvils keeps those inputs on the block, so the exact active-scene pile
 * must be drawn instead for the whole outro.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.AnvilStation", remap = false)
public abstract class AnvilStationOutroMixin {
    @Unique
    private ItemStack cinematiccompat$mainAtExit = ItemStack.EMPTY;

    @Unique
    private ItemStack cinematiccompat$additionAtExit = ItemStack.EMPTY;

    @Inject(method = "onEnd", at = @At("HEAD"), remap = false)
    private void cinematiccompat$captureAnvilInputsBeforeClose(CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.containerMenu == null
                || mc.player.containerMenu.slots.size() < 2) {
            this.cinematiccompat$mainAtExit = ItemStack.EMPTY;
            this.cinematiccompat$additionAtExit = ItemStack.EMPTY;
            return;
        }

        this.cinematiccompat$mainAtExit = mc.player.containerMenu.getSlot(0).getItem().copy();
        this.cinematiccompat$additionAtExit = mc.player.containerMenu.getSlot(1).getItem().copy();
    }

    @Invoker(value = "drawContents", remap = false)
    public static void cinematiccompat$drawContentsForOutro(
            PoseStack pose,
            MultiBufferSource buffers,
            Vec3 camera,
            BlockPos pos,
            Direction side,
            List<ItemStack> items,
            int light) {
        throw new AssertionError("Mixin invoker was not applied");
    }

    @Inject(method = "renderScene", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$renderAnvilPileInsteadOfInventoryFlight(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            int light,
            CallbackInfo ci) {
        if (!CinematicCompatClientHooks.isSceneOutro()) {
            return;
        }

        BlockPos pos = CinematicCompatClientHooks.currentScenePos();
        Direction side = CinematicCompatClientHooks.currentSceneSide();
        if (pos == null || side == null) {
            return;
        }

        cinematiccompat$drawContentsForOutro(
                pose,
                buffers,
                camera.getPosition(),
                pos,
                side,
                List.of(this.cinematiccompat$mainAtExit, this.cinematiccompat$additionAtExit),
                light
        );
        ci.cancel();
    }
}
