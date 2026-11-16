package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.reflect.Method;
import java.util.OptionalInt;
import java.util.function.Supplier;

/** Supplies the target payload expected by Amendments' writable-lectern editor menu. */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.station.MenuActions", remap = false)
public abstract class MenuActionsLecternDataMixin {
    @Redirect(
            method = "open",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/level/ServerPlayer;openMenu(Lnet/minecraft/world/MenuProvider;)Ljava/util/OptionalInt;"
            ),
            remap = false
    )
    private static OptionalInt cinematiccompat$sendLecternTargetWhenNeeded(
            ServerPlayer menuPlayer,
            MenuProvider provider,
            ServerPlayer originalPlayer,
            @Coerce Object session) {
        BlockPos pos = cinematiccompat$getSessionPos(session);
        if (pos == null) {
            return menuPlayer.openMenu(provider);
        }

        BlockEntity blockEntity = originalPlayer.level().getBlockEntity(pos);
        if (blockEntity instanceof LecternBlockEntity lectern
                && lectern.getBook().getItem() instanceof WritableBookItem
                && cinematiccompat$isAmendmentsLecternEditorEnabled()) {
            return menuPlayer.openMenu(provider, buffer -> {
                buffer.writeBoolean(true);
                buffer.writeBlockPos(pos);
            });
        }
        return menuPlayer.openMenu(provider);
    }

    private static BlockPos cinematiccompat$getSessionPos(Object session) {
        if (session == null) {
            return null;
        }
        try {
            Method method = session.getClass().getMethod("pos");
            Object value = method.invoke(session);
            return value instanceof BlockPos pos ? pos : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static boolean cinematiccompat$isAmendmentsLecternEditorEnabled() {
        try {
            Class<?> configs = Class.forName(
                    "net.mehvahdjukaar.amendments.configs.CommonConfigs",
                    true,
                    MenuActionsLecternDataMixin.class.getClassLoader());
            Object value = configs.getField("LECTERN_STUFF").get(null);
            return value instanceof Supplier<?> supplier && Boolean.TRUE.equals(supplier.get());
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }
}
