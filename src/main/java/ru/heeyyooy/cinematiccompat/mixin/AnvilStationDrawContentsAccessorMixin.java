package ru.heeyyooy.cinematiccompat.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * Exposes the exact stacked-item renderer already used by AnvilStation, so
 * Easy Anvils can use the same layout without reflective access to a
 * package-private method in another mod.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.AnvilStation", remap = false)
public interface AnvilStationDrawContentsAccessorMixin {
    @Invoker(value = "drawContents", remap = false)
    static void cinematiccompat$drawContents(
            PoseStack pose,
            MultiBufferSource buffers,
            Vec3 camera,
            BlockPos pos,
            Direction side,
            List<ItemStack> items,
            int light) {
        throw new AssertionError("Mixin invoker was not applied");
    }
}
