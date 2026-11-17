package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allows wood-variant crafting tables to use Cinematic Interactions' table scene. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.table.TableGrid", remap = false)
public abstract class TableGridCompatMixin {
    @Inject(method = "isTable", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cinematiccompat$recognizeCraftingTableVariants(
            BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) {
            return;
        }
        if (state.getBlock() instanceof CraftingTableBlock) {
            cir.setReturnValue(true);
            return;
        }
        var id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if ((id.getNamespace().equals("everycomp") || id.getNamespace().equals("carved_wood"))
                && id.getPath().contains("crafting_table")) {
            cir.setReturnValue(true);
        }
    }
}
