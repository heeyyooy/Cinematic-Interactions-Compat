package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Optional Easy Magic bridge for the enchanting-scene reroll button.
 * All implementation references are resolved by name so this addon still
 * loads when Easy Magic is not installed.
 */
public final class EasyMagicRerollHooks {
    private static final Class<?> MENU_CLASS;
    private static final Method CAN_ENCHANT_ITEM;
    private static final Method CAN_USE_REROLL;

    private static final Method PICKING_MOUSE_GUI;
    private static final Method PICKING_TO_GUI;
    private static final Field STATION_AIM;
    private static final Method STATION_SIDE;

    private static final Method ITEM_PLACER_RENDER;
    private static final Method ENCHANT_FX_CIRCLE;
    private static final Method ENCHANT_FX_BILLBOARD;
    private static final Method ENCHANT_FX_SPARKLE;
    private static final Method ENCHANT_FX_BURST;
    private static final Field ENCHANT_FX_GLOW;
    private static final Class<?> CIRCLE_PALETTE;
    private static final Method SCENE_CLOCK;
    private static final Field SCENE_PHASE;
    private static final Field SCENE_TIME;
    private static final Method SCENE_INTRO_PROGRESS;

    private static final Field EASY_MAGIC_CONFIG;
    private static final Method EASY_MAGIC_CONFIG_GET;
    private static final Field REROLL_ENABLED;

    static {
        ClassLoader loader = EasyMagicRerollHooks.class.getClassLoader();

        Class<?> menuClass = null;
        Method canEnchantItem = null;
        Method canUseReroll = null;

        Method mouseGui = null;
        Method toGui = null;
        Field stationAim = null;
        Method stationSide = null;

        Method itemPlacerRender = null;
        Method enchantFxCircle = null;
        Method enchantFxBillboard = null;
        Method enchantFxSparkle = null;
        Method enchantFxBurst = null;
        Field enchantFxGlow = null;
        Class<?> circlePalette = null;
        Method sceneClock = null;
        Field scenePhase = null;
        Field sceneTime = null;
        Method sceneIntroProgress = null;

        Field easyMagicConfig = null;
        Method easyMagicConfigGet = null;
        Field rerollEnabled = null;

        try {
            menuClass = Class.forName("fuzs.easymagic.world.inventory.ModEnchantmentMenu", false, loader);
            canEnchantItem = menuClass.getMethod("canEnchantItem");
            canUseReroll = menuClass.getMethod("canUseReroll", Player.class);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Easy Magic is optional.
        }

        try {
            Class<?> pickingClass = Class.forName("dev.dyslexic.cinematic.client.Picking", false, loader);
            Class<?> stationClass = Class.forName("dev.dyslexic.cinematic.client.ClientStation", false, loader);
            mouseGui = pickingClass.getMethod("mouseGui");
            toGui = pickingClass.getMethod("toGui", Vec3.class);
            stationAim = stationClass.getField("aim");
            stationSide = stationClass.getMethod("sideDirection");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Cursor/hand integration is disabled if the Cinematic API changes.
        }

        try {
            Class<?> itemPlacer = Class.forName("dev.dyslexic.cinematic.client.ItemPlacer", false, loader);
            itemPlacerRender = itemPlacer.getMethod(
                    "render",
                    PoseStack.class,
                    MultiBufferSource.class,
                    Vec3.class,
                    ItemStack.class,
                    Vec3.class,
                    Direction.class,
                    long.class,
                    float.class,
                    float.class,
                    int.class
            );

            Class<?> enchantFx = Class.forName("dev.dyslexic.cinematic.client.EnchantFx", false, loader);
            circlePalette = Class.forName(
                    "dev.dyslexic.cinematic.client.EnchantFx$CirclePalette", false, loader);
            enchantFxCircle = enchantFx.getMethod(
                    "circle",
                    PoseStack.class,
                    MultiBufferSource.class,
                    Vec3.class,
                    Vec3.class,
                    double.class,
                    double.class,
                    circlePalette,
                    float.class
            );
            enchantFxBillboard = enchantFx.getMethod(
                    "billboard",
                    PoseStack.class,
                    MultiBufferSource.class,
                    Camera.class,
                    Vec3.class,
                    double.class,
                    double.class,
                    net.minecraft.resources.ResourceLocation.class,
                    float.class,
                    float.class,
                    float.class
            );
            enchantFxSparkle = enchantFx.getMethod("sparkle", Vec3.class, double.class);
            enchantFxBurst = enchantFx.getMethod("burst", Vec3.class, double.class);
            enchantFxGlow = enchantFx.getField("GLOW");

        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Physical item and rune effects are optional decoration.
        }

        try {
            Class<?> sceneClass = Class.forName("dev.dyslexic.cinematic.client.Scene", false, loader);
            sceneClock = sceneClass.getMethod("clock");
            scenePhase = sceneClass.getField("phase");
            sceneTime = sceneClass.getField("time");
            sceneIntroProgress = sceneClass.getMethod("introProgress");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Clock animation falls back to the player tick count; scene transitions stay disabled.
        }

        try {
            Class<?> easyMagicClass = Class.forName("fuzs.easymagic.EasyMagic", false, loader);
            Class<?> serverConfigClass = Class.forName("fuzs.easymagic.config.ServerConfig", false, loader);
            easyMagicConfig = easyMagicClass.getField("CONFIG");
            easyMagicConfigGet = easyMagicConfig.getType().getMethod("get", Class.class);
            rerollEnabled = serverConfigClass.getField("rerollEnchantments");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // If Easy Magic's config API changes, menu validation remains the final authority.
        }

        MENU_CLASS = menuClass;
        CAN_ENCHANT_ITEM = canEnchantItem;
        CAN_USE_REROLL = canUseReroll;
        PICKING_MOUSE_GUI = mouseGui;
        PICKING_TO_GUI = toGui;
        STATION_AIM = stationAim;
        STATION_SIDE = stationSide;
        ITEM_PLACER_RENDER = itemPlacerRender;
        ENCHANT_FX_CIRCLE = enchantFxCircle;
        ENCHANT_FX_BILLBOARD = enchantFxBillboard;
        ENCHANT_FX_SPARKLE = enchantFxSparkle;
        ENCHANT_FX_BURST = enchantFxBurst;
        ENCHANT_FX_GLOW = enchantFxGlow;
        CIRCLE_PALETTE = circlePalette;
        SCENE_CLOCK = sceneClock;
        SCENE_PHASE = scenePhase;
        SCENE_TIME = sceneTime;
        SCENE_INTRO_PROGRESS = sceneIntroProgress;
        EASY_MAGIC_CONFIG = easyMagicConfig;
        EASY_MAGIC_CONFIG_GET = easyMagicConfigGet;
        REROLL_ENABLED = rerollEnabled;
    }

    private EasyMagicRerollHooks() {
    }

    private static Object menu() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || MENU_CLASS == null || !MENU_CLASS.isInstance(player.containerMenu)) {
            return null;
        }
        return player.containerMenu;
    }

    /** Same visibility rule as Easy Magic's RerollButton.tick(). */
    public static boolean isButtonVisible() {
        Object menu = menu();
        if (menu == null || CAN_ENCHANT_ITEM == null || !isRerollEnabled()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(CAN_ENCHANT_ITEM.invoke(menu));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /** Same active-state rule as Easy Magic's RerollButton.tick(). */
    public static boolean canReroll() {
        Object menu = menu();
        LocalPlayer player = Minecraft.getInstance().player;
        if (menu == null || player == null || CAN_USE_REROLL == null || !isRerollEnabled()) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(CAN_USE_REROLL.invoke(menu, player));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static boolean isRerollEnabled() {
        if (EASY_MAGIC_CONFIG == null || EASY_MAGIC_CONFIG_GET == null || REROLL_ENABLED == null) {
            // If configuration introspection is unavailable, let the menu's
            // own clickMenuButton implementation accept or reject the action.
            return true;
        }
        try {
            Object holder = EASY_MAGIC_CONFIG.get(null);
            Object config = EASY_MAGIC_CONFIG_GET.invoke(holder, REROLL_ENABLED.getDeclaringClass());
            return Boolean.TRUE.equals(REROLL_ENABLED.get(config));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Mirrors ModEnchantmentScreen's callback. Easy Magic validates the action,
     * applies its configured XP/catalyst costs and refreshes its own clues.
     */
    public static boolean clickReroll() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.gameMode == null || !isButtonVisible() || !canReroll()
                || !(player.containerMenu instanceof EnchantmentMenu menu)) {
            return false;
        }
        if (!menu.clickMenuButton(player, 4)) {
            return false;
        }
        mc.gameMode.handleInventoryButtonClick(menu.containerId, 4);
        return true;
    }

    public static boolean isCursorOver(Vec3 point, double radiusBlocks) {
        if (point == null || PICKING_MOUSE_GUI == null || PICKING_TO_GUI == null) {
            return false;
        }
        try {
            double[] mouse = (double[]) PICKING_MOUSE_GUI.invoke(null);
            double[] center = (double[]) PICKING_TO_GUI.invoke(null, point);
            double[] edge = (double[]) PICKING_TO_GUI.invoke(null, point.add(0.0, radiusBlocks, 0.0));
            if (mouse == null || center == null || edge == null) {
                return false;
            }
            double radius = Math.max(8.0, Math.hypot(center[0] - edge[0], center[1] - edge[1]));
            return Math.hypot(mouse[0] - center[0], mouse[1] - center[1]) <= radius;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    public static boolean hasAim(Object station) {
        if (station == null || STATION_AIM == null) {
            return false;
        }
        try {
            return STATION_AIM.get(station) != null;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return false;
        }
    }

    public static void setAim(Object station, Vec3 aim) {
        if (station == null || aim == null || STATION_AIM == null) {
            return;
        }
        try {
            STATION_AIM.set(station, aim);
        } catch (IllegalAccessException | RuntimeException ignored) {
            // The click remains usable if only the hand-aim bridge changes.
        }
    }

    public static Direction stationSide(Object station) {
        if (station == null || STATION_SIDE == null) {
            return Direction.NORTH;
        }
        try {
            Object value = STATION_SIDE.invoke(station);
            return value instanceof Direction direction ? direction : Direction.NORTH;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return Direction.NORTH;
        }
    }

    public static double clock() {
        if (SCENE_CLOCK != null) {
            try {
                Object value = SCENE_CLOCK.invoke(null);
                if (value instanceof Number number) {
                    return number.doubleValue();
                }
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Fall through to the local tick clock.
            }
        }
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? 0.0 : player.tickCount / 20.0;
    }

    private static double smoothstep(double value) {
        double t = Math.max(0.0, Math.min(1.0, value));
        return t * t * (3.0 - 2.0 * t);
    }

    private static String scenePhase() {
        if (SCENE_PHASE == null) {
            return "ACTIVE";
        }
        try {
            Object phase = SCENE_PHASE.get(null);
            return phase == null ? "NONE" : phase.toString();
        } catch (IllegalAccessException | RuntimeException ignored) {
            return "ACTIVE";
        }
    }

    private static double introProgress() {
        if (SCENE_INTRO_PROGRESS == null) {
            return 1.0;
        }
        try {
            Object value = SCENE_INTRO_PROGRESS.invoke(null);
            return value instanceof Number number ? number.doubleValue() : 1.0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 1.0;
        }
    }

    private static double outroProgress() {
        if (SCENE_TIME == null) {
            return 0.0;
        }
        try {
            Object value = SCENE_TIME.get(null);
            return value instanceof Number number ? number.doubleValue() / 0.45 : 0.0;
        } catch (IllegalAccessException | RuntimeException ignored) {
            return 0.0;
        }
    }

    /** Opacity of the rune base: it fades with the actual scene intro/outro. */
    private static float backgroundPresence() {
        return switch (scenePhase()) {
            case "INTRO" -> (float) smoothstep(introProgress());
            case "ACTIVE" -> 1.0f;
            case "OUTRO" -> (float) (1.0 - smoothstep(outroProgress()));
            default -> 0.0f;
        };
    }

    /** The clock grows in during intro and shrinks away during outro. */
    private static float clockPresence() {
        return switch (scenePhase()) {
            case "INTRO" -> {
                double t = Math.max(0.0, Math.min(1.0, introProgress()));
                // Ease-out cubic: starts small and settles exactly at its normal size.
                yield (float) (1.0 - Math.pow(1.0 - t, 3.0));
            }
            case "ACTIVE" -> 1.0f;
            case "OUTRO" -> (float) (1.0 - smoothstep(outroProgress()));
            default -> 0.0f;
        };
    }

    /**
     * A physical reroll token: a dropped clock model, surrounded by the same
     * pixel-rune and glow effects used by the enchanting offers.
     */
    public static void renderButton(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            Vec3 position,
            Direction facing,
            boolean hovered,
            boolean affordable,
            double flashAge,
            int light) {
        if (pose == null || buffers == null || camera == null || position == null) {
            return;
        }

        float background = backgroundPresence();
        float clockScale = clockPresence();
        if (background <= 0.005f && clockScale <= 0.005f) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        double clock = clock();
        float hoverScale = hovered ? 1.10f : 1.0f;
        float baseBrightness = affordable ? (hovered ? 0.92f : 0.68f) : (hovered ? 0.58f : 0.38f);
        // Blue lapis with a smaller violet inner rune complements the gold clock
        // without the harsh magenta/red contrast of the earlier pentagram.
        Object outerPalette = palette("LAPIS");
        Object innerPalette = palette("VIOLET");
        Vec3 center = position.add(0.0, 0.006, 0.0);

        // The rune background has its own opacity animation, independent of the clock scale.
        if (background > 0.005f) {
            float brightness = baseBrightness * background;
            drawCircle(pose, buffers, cameraPos, center,
                    0.132 * hoverScale, clock * (hovered ? 0.78 : 0.30), outerPalette, brightness);
            drawCircle(pose, buffers, cameraPos, center.add(0.0, 0.001, 0.0),
                    0.078 * hoverScale, -clock * 0.52, innerPalette, brightness * 0.74f);
            drawGlow(pose, buffers, camera, center.add(0.0, 0.018, 0.0),
                    0.15 * hoverScale,
                    0.18f * brightness,
                    0.46f * brightness,
                    0.95f * brightness,
                    1.0f);

            if (flashAge >= 0.0 && flashAge < 0.65) {
                float flash = (float) (1.0 - flashAge / 0.65) * background;
                drawCircle(pose, buffers, cameraPos, center.add(0.0, 0.003, 0.0),
                        0.15 + 0.06 * (1.0 - flash), clock * 1.8, outerPalette, flash * 0.95f);
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (background > 0.45f && player != null
                    && player.tickCount % 7 == 0 && ENCHANT_FX_SPARKLE != null) {
                try {
                    ENCHANT_FX_SPARKLE.invoke(null, center.add(0.0, 0.025, 0.0), 0.09);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    // Purely decorative.
                }
            }
        }

        // The gold clock is deliberately smaller than the rune base and has
        // its own scale animation on entering and leaving the scene.
        if (clockScale > 0.005f && ITEM_PLACER_RENDER != null) {
            try {
                ITEM_PLACER_RENDER.invoke(
                        null,
                        pose,
                        buffers,
                        cameraPos,
                        new ItemStack(Items.CLOCK),
                        position.add(0.0, 0.009, 0.0),
                        facing == null ? Direction.NORTH : facing,
                        0x5245524F4C4CL,
                        0.62f * clockScale * hoverScale,
                        0.0f,
                        light
                );
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Rune and glow remain visible if the optional item-model bridge changes.
            }
        }
    }

    public static void playRerollBurst(Vec3 position) {
        if (position != null && ENCHANT_FX_BURST != null) {
            try {
                ENCHANT_FX_BURST.invoke(null, position, 0.55);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Sound and the actual reroll action do not depend on particles.
            }
        }
    }

    private static Object palette(String name) {
        if (CIRCLE_PALETTE == null) {
            return null;
        }
        try {
            Object[] values = CIRCLE_PALETTE.getEnumConstants();
            if (values != null) {
                for (Object value : values) {
                    if (value instanceof Enum<?> entry && entry.name().equals(name)) {
                        return value;
                    }
                }
            }
        } catch (RuntimeException ignored) {
            // No palette means the decoration is skipped.
        }
        return null;
    }

    private static void drawCircle(
            PoseStack pose,
            MultiBufferSource buffers,
            Vec3 camera,
            Vec3 center,
            double radius,
            double angle,
            Object palette,
            float brightness) {
        if (ENCHANT_FX_CIRCLE == null || palette == null) {
            return;
        }
        try {
            ENCHANT_FX_CIRCLE.invoke(null, pose, buffers, camera, center,
                    radius, angle, palette, brightness);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Decoration only.
        }
    }

    private static void drawGlow(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            Vec3 center,
            double radius,
            float red,
            float green,
            float blue,
            float brightness) {
        if (ENCHANT_FX_BILLBOARD == null || ENCHANT_FX_GLOW == null) {
            return;
        }
        try {
            Object glow = ENCHANT_FX_GLOW.get(null);
            ENCHANT_FX_BILLBOARD.invoke(null, pose, buffers, camera, center, radius, 0.0,
                    glow, red * brightness, green * brightness, blue * brightness);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Decoration only.
        }
    }
}
