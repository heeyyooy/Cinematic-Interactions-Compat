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
 * Small runtime bridge to Cinematic Interactions and Easy Anvils.
 * It lives outside the reserved mixin package.
 */
public final class CinematicCompatClientHooks {
    private static final Method ACTIVE_METHOD;
    private static final Field STATION_FIELD;
    private static final Field STATION_POS_FIELD;
    private static final Field PHASE_FIELD;
    private static final Field TIME_FIELD;
    private static final Method STATION_KIND_METHOD;
    private static final Method STATION_SIDE_METHOD;
    private static final Method ANVIL_DRAW_CONTENTS_METHOD;
    private static final Method EASY_ANVILS_CONFIG_GET_METHOD;
    private static final Field EASY_ANVILS_CONFIG_FIELD;
    private static final Field EASY_ANVILS_RENDER_CONTENTS_FIELD;

    static {
        Method activeMethod = null;
        Field stationField = null;
        Field stationPosField = null;
        Field phaseField = null;
        Field timeField = null;
        Method stationKindMethod = null;
        Method stationSideMethod = null;
        Method anvilDrawContentsMethod = null;
        Method easyAnvilsConfigGetMethod = null;
        Field easyAnvilsConfigField = null;
        Field easyAnvilsRenderContentsField = null;

        try {
            ClassLoader loader = CinematicCompatClientHooks.class.getClassLoader();
            Class<?> sceneClass = Class.forName("dev.dyslexic.cinematic.client.Scene", false, loader);
            Class<?> stationClass = Class.forName("dev.dyslexic.cinematic.client.ClientStation", false, loader);
            Class<?> anvilStationClass = Class.forName("dev.dyslexic.cinematic.client.AnvilStation", false, loader);

            activeMethod = sceneClass.getMethod("active");
            stationField = sceneClass.getField("station");
            stationPosField = stationClass.getField("pos");
            phaseField = sceneClass.getField("phase");
            timeField = sceneClass.getField("time");
            stationKindMethod = stationClass.getMethod("kind");
            stationSideMethod = stationClass.getMethod("sideDirection");
            // Public @Invoker added by AnvilStationOutroMixin. A public method
            // avoids setAccessible and works with NeoForge's module boundaries.
            anvilDrawContentsMethod = anvilStationClass.getMethod(
                    "cinematiccompat$drawContentsForOutro",
                    PoseStack.class,
                    MultiBufferSource.class,
                    Vec3.class,
                    BlockPos.class,
                    Direction.class,
                    List.class,
                    int.class
            );
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The optional Cinematic Interactions integration is inactive if its API is unavailable.
        }

        try {
            ClassLoader loader = CinematicCompatClientHooks.class.getClassLoader();
            Class<?> easyAnvilsClass = Class.forName("fuzs.easyanvils.EasyAnvils", false, loader);
            Class<?> clientConfigClass = Class.forName("fuzs.easyanvils.config.ClientConfig", false, loader);

            easyAnvilsConfigField = easyAnvilsClass.getField("CONFIG");
            easyAnvilsConfigGetMethod = easyAnvilsConfigField.getType().getMethod("get", Class.class);
            easyAnvilsRenderContentsField = clientConfigClass.getField("renderAnvilContents");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // If Easy Anvils changes its config API, preserve its default rendering behavior.
        }

        ACTIVE_METHOD = activeMethod;
        STATION_FIELD = stationField;
        STATION_POS_FIELD = stationPosField;
        PHASE_FIELD = phaseField;
        TIME_FIELD = timeField;
        STATION_KIND_METHOD = stationKindMethod;
        STATION_SIDE_METHOD = stationSideMethod;
        ANVIL_DRAW_CONTENTS_METHOD = anvilDrawContentsMethod;
        EASY_ANVILS_CONFIG_GET_METHOD = easyAnvilsConfigGetMethod;
        EASY_ANVILS_CONFIG_FIELD = easyAnvilsConfigField;
        EASY_ANVILS_RENDER_CONTENTS_FIELD = easyAnvilsRenderContentsField;
    }

    private CinematicCompatClientHooks() {
    }

    public static boolean isSceneActive() {
        if (ACTIVE_METHOD == null) return false;
        try {
            return Boolean.TRUE.equals(ACTIVE_METHOD.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
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

    public static boolean isSceneOutro() {
        if (PHASE_FIELD == null) return false;
        try {
            Object phase = PHASE_FIELD.get(null);
            return phase != null && "OUTRO".equals(phase.toString());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean isAnvilOutro() {
        if (!isSceneOutro() || STATION_FIELD == null || STATION_KIND_METHOD == null) return false;
        try {
            Object station = STATION_FIELD.get(null);
            if (station == null) return false;
            Object kind = STATION_KIND_METHOD.invoke(station);
            return kind != null && "ANVIL".equals(kind.toString());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static BlockPos currentScenePos() {
        if (STATION_FIELD == null || STATION_POS_FIELD == null) return null;
        try {
            Object station = STATION_FIELD.get(null);
            return station == null ? null : (BlockPos) STATION_POS_FIELD.get(station);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static Direction currentSceneSide() {
        if (STATION_FIELD == null || STATION_SIDE_METHOD == null) return null;
        try {
            Object station = STATION_FIELD.get(null);
            return station == null ? null : (Direction) STATION_SIDE_METHOD.invoke(station);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    public static boolean easyAnvilsRenderContentsEnabled() {
        if (EASY_ANVILS_CONFIG_FIELD == null || EASY_ANVILS_CONFIG_GET_METHOD == null
                || EASY_ANVILS_RENDER_CONTENTS_FIELD == null) {
            return true;
        }
        try {
            Object holder = EASY_ANVILS_CONFIG_FIELD.get(null);
            Object config = EASY_ANVILS_CONFIG_GET_METHOD.invoke(
                    holder, EASY_ANVILS_RENDER_CONTENTS_FIELD.getDeclaringClass());
            return Boolean.TRUE.equals(EASY_ANVILS_RENDER_CONTENTS_FIELD.get(config));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return true;
        }
    }

    /** Use from Easy Anvils' block-local BlockEntityRenderer pose stack. */
    public static boolean drawAnvilContentsAtBlockOrigin(
            PoseStack pose, MultiBufferSource buffers, Direction side, List<ItemStack> items, int light) {
        return drawAnvilContents(pose, buffers, Vec3.ZERO, BlockPos.ZERO, side, items, light);
    }

    /** Use from a camera-relative world render pass. */
    public static boolean drawAnvilContents(
            PoseStack pose, MultiBufferSource buffers, Vec3 camera, BlockPos pos,
            Direction side, List<ItemStack> items, int light) {
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

    /** Quintic smooth-step progress matching Cinematic Interactions' Anim.smoother easing. */
    public static float outroProgress(double durationSeconds) {
        if (!isSceneOutro() || TIME_FIELD == null || durationSeconds <= 0.0) return 0.0f;
        try {
            Object value = TIME_FIELD.get(null);
            if (!(value instanceof Number number)) return 0.0f;
            double t = Math.max(0.0, Math.min(1.0, number.doubleValue() / durationSeconds));
            return (float) (t * t * t * (t * (t * 6.0 - 15.0) + 10.0));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 0.0f;
        }
    }
}
