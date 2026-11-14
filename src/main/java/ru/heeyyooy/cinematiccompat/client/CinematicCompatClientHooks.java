package ru.heeyyooy.cinematiccompat.client;

import net.minecraft.core.BlockPos;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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

    static {
        Method activeMethod = null;
        Field stationField = null;
        Field stationPosField = null;

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

            activeMethod = sceneClass.getMethod("active");
            stationField = sceneClass.getField("station");
            stationPosField = stationClass.getField("pos");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            // The optional client integration is simply inactive when the host is unavailable.
        }

        ACTIVE_METHOD = activeMethod;
        STATION_FIELD = stationField;
        STATION_POS_FIELD = stationPosField;
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
}
