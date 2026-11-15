package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds modded workstation blocks to Cinematic Interactions' own station classifier.
 * The target mod is referenced by name so the original mod JAR is not bundled or
 * needed as a compile-time library. The hard dependency is declared in mods.toml.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.station.StationKind", remap = false)
public abstract class StationKindCompatMixin {
    @Inject(method = "of", at = @At("RETURN"), cancellable = true, remap = false)
    private static void cinematiccompat$recognizeModdedWorkstations(
            BlockState state, CallbackInfoReturnable<Object> cir) {
        if (cir.getReturnValue() != null) {
            return;
        }

        String kind = cinematiccompat$classify(state);
        if (kind != null) {
            cir.setReturnValue(cinematiccompat$stationKind(kind));
        }
    }

    private static Object cinematiccompat$stationKind(String name) {
        try {
            Class<?> type = Class.forName(
                    "dev.dyslexic.cinematic.station.StationKind",
                    false,
                    StationKindCompatMixin.class.getClassLoader());
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object value = Enum.valueOf((Class<? extends Enum>) type.asSubclass(Enum.class), name);
            return value;
        } catch (ReflectiveOperationException | IllegalArgumentException exception) {
            throw new IllegalStateException("Cinematic Interactions station kind is unavailable: " + name, exception);
        }
    }

    private static String cinematiccompat$classify(BlockState state) {
        var block = state.getBlock();

        if (block instanceof CraftingTableBlock) {
            return "TABLE";
        }
        if (block instanceof AbstractFurnaceBlock && state.hasProperty(AbstractFurnaceBlock.FACING)) {
            return "FURNACE";
        }
        if (block instanceof EnchantingTableBlock) {
            return "ENCHANTING";
        }
        if (block instanceof AnvilBlock && state.hasProperty(AnvilBlock.FACING)) {
            return "ANVIL";
        }
        if (block instanceof LecternBlock
                && state.hasProperty(LecternBlock.HAS_BOOK)
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return "LECTERN";
        }
        if (block instanceof ChiseledBookShelfBlock
                && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
            return "CHISELED_BOOKSHELF";
        }

        // Every Compat variants may use wrapper blocks rather than vanilla subclasses.
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        String namespace = id.getNamespace();
        String path = id.getPath();

        if (namespace.equals("everycomp") || namespace.equals("carved_wood")) {
            if (path.contains("crafting_table")) {
                return "TABLE";
            }
            if (path.contains("lectern")
                    && state.hasProperty(LecternBlock.HAS_BOOK)
                    && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                return "LECTERN";
            }
            if (path.contains("chiseled_bookshelf")
                    && state.hasProperty(HorizontalDirectionalBlock.FACING)) {
                return "CHISELED_BOOKSHELF";
            }
        }

        if (namespace.equals("quark") && path.contains("furnace")
                && state.hasProperty(AbstractFurnaceBlock.FACING)) {
            return "FURNACE";
        }
        if (namespace.equals("easyanvils") && path.contains("anvil")
                && state.hasProperty(AnvilBlock.FACING)) {
            return "ANVIL";
        }
        if (namespace.equals("easymagic")
                && (path.contains("enchanting_table") || path.contains("enchantment_table"))) {
            return "ENCHANTING";
        }
        if (namespace.equals("caverns_and_chasms") && path.contains("bejeweled_anvil")
                && state.hasProperty(AnvilBlock.FACING)) {
            return "ANVIL";
        }
        return null;
    }
}
