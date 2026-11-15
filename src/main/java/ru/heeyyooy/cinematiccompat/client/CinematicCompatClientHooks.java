package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Accesses the current Cinematic Interactions scene without making its client
 * implementation a compile-time dependency of this compatibility addon.
 *
 * This class deliberately lives outside the mixin package: Mixin reserves that
 * package for mixin classes and rejects ordinary classes loaded from it.
 */
public final class CinematicCompatClientHooks {
    private static final Method ACTIVE_METHOD;
    private static final Field STATION_FIELD;
    private static final Field STATION_POS_FIELD;
    private static final Field STATION_SIDE_FIELD;
    private static final Field PHASE_FIELD;
    private static final Field TIME_FIELD;
    private static final Method STATION_KIND_METHOD;
    private static final Method ANVIL_DRAW_CONTENTS_METHOD;

    static {
        Method activeMethod = null;
        Field stationField = null;
        Field stationPosField = null;
        Field stationSideField = null;
        Field phaseField = null;
        Field timeField = null;
        Method stationKindMethod = null;
        Method anvilDrawContentsMethod = null;

        try {
            ClassLoader loader = CinematicCompatClientHooks.class.getClassLoader();
            Class<?> sceneClass = Class.forName(
                    "dev.dyslexic.cinematic.client.Scene",
                    false,
                    loader);
            Class<?> stationClass = Class.forName(
                    "dev.dyslexic.cinematic.client.ClientStation",
                    false,
                    loader);
            Class<?> anvilStationClass = Class.forName(
                    "dev.dyslexic.cinematic.client.AnvilStation",
                    false,
                    loader);

            activeMethod = sceneClass.getMethod("active");
            stationField = sceneClass.getField("station");
            stationPosField = stationClass.getField("pos");
            stationSideField = stationClass.getDeclaredField("side");
            stationSideField.setAccessible(true);
            phaseField = sceneClass.getField("phase");
            timeField = sceneClass.getField("time");
            stationKindMethod = stationClass.getMethod("kind");

            anvilDrawContentsMethod = anvilStationClass.getDeclaredMethod(
                    "drawContents",
                    PoseStack.class,
                    MultiBufferSource.class,
                    Vec3.class,
                    BlockPos.class,
                    Direction.class,
                    List.class,
                    int.class
            );
            anvilDrawContentsMethod.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The optional client integration is simply inactive when the host is unavailable.
        }

        ACTIVE_METHOD = activeMethod;
        STATION_FIELD = stationField;
        STATION_POS_FIELD = stationPosField;
        STATION_SIDE_FIELD = stationSideField;
        PHASE_FIELD = phaseField;
        TIME_FIELD = timeField;
        STATION_KIND_METHOD = stationKindMethod;
        ANVIL_DRAW_CONTENTS_METHOD = anvilDrawContentsMethod;
    }

    private CinematicCompatClientHooks() {
    }

    public static boolean isSceneActiveAt(BlockPos pos) {
        if (pos == null || ACTIVE_METHOD == null || STATION_FIELD == null || STATION_POS_FIELD == null) {
            return false;
        }

        try {
            if (!Boolean.TRUE.equals(ACTIVE_METHOD.invoke(null))) {
                return false;
            }

            Object station = STATION_FIELD.get(null);
            return station != null && pos.equals(STATION_POS_FIELD.get(station));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** True while the current Cinematic Interactions scene is in its exit phase. */
    public static boolean isSceneOutro() {
        if (PHASE_FIELD == null) {
            return false;
        }

        try {
            Object phase = PHASE_FIELD.get(null);
            return phase != null && "OUTRO".equals(phase.toString());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** True only while an anvil scene is playing its exit animation. */
    public static boolean isAnvilOutro() {
        if (!isSceneOutro() || STATION_FIELD == null || STATION_KIND_METHOD == null) {
            return false;
        }

        try {
            Object station = STATION_FIELD.get(null);
            if (station == null) {
                return false;
            }
            Object kind = STATION_KIND_METHOD.invoke(station);
            return kind != null && "ANVIL".equals(kind.toString());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Renders the same stacked, three-dimensional anvil items as the
     * Cinematic Interactions scene. This keeps their position and orientation
     * identical after the cinematic camera has returned to normal.
     */
    public static boolean drawAnvilContents(
            PoseStack pose,
            MultiBufferSource buffers,
            Vec3 camera,
            BlockPos pos,
            Direction side,
            List<ItemStack> items,
            int light) {
        if (ANVIL_DRAW_CONTENTS_METHOD == null || pose == null || buffers == null
                || camera == null || pos == null || side == null || items == null) {
            return false;
        }

        try {
            ANVIL_DRAW_CONTENTS_METHOD.invoke(null, pose, buffers, camera, pos, side, items, light);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** Uses the current anvil scene and its saved menu contents during the outro. */
    public static boolean drawCurrentAnvilContents(
            PoseStack pose,
            MultiBufferSource buffers,
            Vec3 camera,
            List<ItemStack> items,
            int light) {
        if (STATION_FIELD == null || STATION_POS_FIELD == null || STATION_SIDE_FIELD == null) {
            return false;
        }

        try {
            Object station = STATION_FIELD.get(null);
            if (station == null) {
                return false;
            }

            BlockPos pos = (BlockPos) STATION_POS_FIELD.get(station);
            Direction side = (Direction) STATION_SIDE_FIELD.get(station);
            return drawAnvilContents(pose, buffers, camera, pos, side, items, light);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Quintic smooth-step progress over the requested duration. It matches
     * Cinematic Interactions' Anim.smoother easing used by the scene outro.
     */
    public static float outroProgress(double durationSeconds) {
        if (!isSceneOutro() || TIME_FIELD == null || durationSeconds <= 0.0) {
            return 0.0f;
        }

        try {
            Object value = TIME_FIELD.get(null);
            if (!(value instanceof Number number)) {
                return 0.0f;
            }

            double t = Math.max(0.0, Math.min(1.0, number.doubleValue() / durationSeconds));
            double eased = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
            return (float) eased;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 0.0f;
        }
    }
}
