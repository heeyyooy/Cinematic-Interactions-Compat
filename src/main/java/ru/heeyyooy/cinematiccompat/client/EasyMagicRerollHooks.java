package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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
    private static volatile boolean handTapRequested;
    private static volatile double rerollDeniedAt = -10.0;
    // Keep 80% of the original violet rune and 85% of the original blue rune.
    private static final double VIOLET_SOCKET_REDUCTION = 0.20;
    private static final double BLUE_PERIMETER_RADIUS = 0.11;
    private static final double VIOLET_SOCKET_RADIUS = 0.10 * (1.0 - VIOLET_SOCKET_REDUCTION);
    private static final double BLUE_GLOW_SCALE = BLUE_PERIMETER_RADIUS / 0.115;
    private static final double CLOCK_CENTER_LEFT_OFFSET = 0.012;
    private static final double CLOCK_CENTER_AWAY_OFFSET = 0.012;
    private static final double SOCKET_CIRCLE_LIFT = 0.016;
    private static final double OUTER_CIRCLE_LIFT = 0.012;
    private static final float CLOCK_MODEL_SCALE = 0.713f;
    private static final Class<?> MENU_CLASS;
    private static final Method CAN_ENCHANT_ITEM;
    private static final Method CAN_USE_REROLL;

    private static final Method PICKING_MOUSE_GUI;
    private static final Method PICKING_TO_GUI;
    private static final Field STATION_AIM;
    private static final Method STATION_SIDE;
    private static final Method STATION_GEOMETRY;
    private static final Method GEOMETRY_SCALE;

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
    private static final Field REROLL_CATALYST_COST;
    private static final Field REROLL_EXPERIENCE_COST;
    private static final Field REROLLING_TAKES_LEVELS;
    private static final Field DEDICATED_REROLL_CATALYST;

    static {
        ClassLoader loader = EasyMagicRerollHooks.class.getClassLoader();

        Class<?> menuClass = null;
        Method canEnchantItem = null;
        Method canUseReroll = null;

        Method mouseGui = null;
        Method toGui = null;
        Field stationAim = null;
        Method stationSide = null;
        Method stationGeometry = null;
        Method geometryScale = null;

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
        Field rerollCatalystCost = null;
        Field rerollExperienceCost = null;
        Field rerollingTakesLevels = null;
        Field dedicatedRerollCatalyst = null;

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
            Class<?> spatialTargetClass = Class.forName(
                    "dev.dyslexic.cinematic.client.SpatialTarget", false, loader);
            Class<?> spatialAppearanceClass = Class.forName(
                    "dev.dyslexic.cinematic.client.SpatialAppearance", false, loader);
            stationGeometry = spatialTargetClass.getMethod("geometry");
            geometryScale = spatialAppearanceClass.getDeclaredMethod("scale", int.class);
            geometryScale.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The vanilla socket proportions remain the safe fallback.
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
            rerollCatalystCost = serverConfigClass.getField("rerollCatalystCost");
            rerollExperienceCost = serverConfigClass.getField("rerollExperiencePointsCost");
            rerollingTakesLevels = serverConfigClass.getField("rerollingTakesEnchantmentLevels");
            dedicatedRerollCatalyst = serverConfigClass.getField("dedicatedRerollCatalyst");
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
        STATION_GEOMETRY = stationGeometry;
        GEOMETRY_SCALE = geometryScale;
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
        REROLL_CATALYST_COST = rerollCatalystCost;
        REROLL_EXPERIENCE_COST = rerollExperienceCost;
        REROLLING_TAKES_LEVELS = rerollingTakesLevels;
        DEDICATED_REROLL_CATALYST = dedicatedRerollCatalyst;
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

    private static Object serverConfig() {
        if (EASY_MAGIC_CONFIG == null || EASY_MAGIC_CONFIG_GET == null || REROLL_ENABLED == null) {
            return null;
        }
        try {
            Object holder = EASY_MAGIC_CONFIG.get(null);
            return EASY_MAGIC_CONFIG_GET.invoke(holder, REROLL_ENABLED.getDeclaringClass());
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static int configInt(Field field) {
        Object config = serverConfig();
        if (config == null || field == null) return -1;
        try {
            return field.getInt(config);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return -1;
        }
    }

    private static boolean configBoolean(Field field, boolean fallback) {
        Object config = serverConfig();
        if (config == null || field == null) return fallback;
        try {
            return field.getBoolean(config);
        } catch (IllegalAccessException | RuntimeException ignored) {
            return fallback;
        }
    }

    /** Human-readable name of the actual Easy Magic catalyst selected by its server config. */
    private static String catalystName() {
        boolean dedicated = configBoolean(DEDICATED_REROLL_CATALYST, false);
        Object activeMenu = menu();
        if (dedicated && activeMenu instanceof EnchantmentMenu enchantmentMenu) {
            try {
                ItemStack stack = enchantmentMenu.getSlot(2).getItem();
                if (!stack.isEmpty()) {
                    return stack.getHoverName().getString();
                }
            } catch (RuntimeException ignored) {
                // A custom dedicated catalyst may not currently occupy its slot.
            }
            return Component.translatable("cinematiccompat.reroll.cost.generic_catalyst").getString();
        }
        return new ItemStack(Items.LAPIS_LAZULI).getHoverName().getString();
    }

    /**
     * The bottom scene hint combines the native reroll action with the actual
     * configured costs. Creative players do not pay these costs, so hide them.
     */
    public static Component rerollHint() {
        Component base = Component.translatable("cinematiccompat.hint.reroll");
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.getAbilities().instabuild) {
            return base;
        }

        int catalystCost = configInt(REROLL_CATALYST_COST);
        int experienceCost = configInt(REROLL_EXPERIENCE_COST);
        if (catalystCost < 0 || experienceCost < 0) {
            return base;
        }

        boolean takesLevels = configBoolean(REROLLING_TAKES_LEVELS, false);
        var hint = Component.translatable("cinematiccompat.hint.reroll");
        if (catalystCost > 0) {
            hint.append(Component.literal("  · ").withStyle(ChatFormatting.DARK_GRAY));
            hint.append(Component.translatable(
                    "cinematiccompat.reroll.cost.catalyst",
                    catalystName(),
                    catalystCost
            ).withStyle(ChatFormatting.AQUA));
        }
        if (experienceCost > 0) {
            hint.append(Component.literal("  · ").withStyle(ChatFormatting.DARK_GRAY));
            hint.append(Component.translatable(
                    takesLevels
                            ? "cinematiccompat.reroll.cost.levels"
                            : "cinematiccompat.reroll.cost.experience",
                    experienceCost
            ).withStyle(takesLevels ? ChatFormatting.GOLD : ChatFormatting.GREEN));
        }
        return hint;
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

    private static double lapisSocketScale(Object station) {
        if (station == null || STATION_GEOMETRY == null || GEOMETRY_SCALE == null) {
            return 1.0;
        }
        try {
            Object geometry = STATION_GEOMETRY.invoke(station);
            Object value = geometry == null ? null : GEOMETRY_SCALE.invoke(geometry, 1);
            return value instanceof Number number ? Math.max(0.25, Math.min(2.5, number.doubleValue())) : 1.0;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return 1.0;
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

    public static boolean isSceneOutro() {
        return "OUTRO".equals(scenePhase());
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

    /** Store the failed-attempt time, matching EnchantStation's denyAt effect. */
    public static void markRerollDenied() {
        rerollDeniedAt = clock();
    }

    /** Exact horizontal denial shake used by EnchantStation.sigilPos(). */
    private static Vec3 denialShake(Vec3 position, Direction facing, double now) {
        double age = now - rerollDeniedAt;
        if (age < 0.0 || age >= 0.35 || facing == null) {
            return position;
        }

        Vec3 right = Vec3.atLowerCornerOf(facing.getOpposite().getClockWise().getNormal());
        double offset = 0.02 * Math.sin(age * 60.0) * (1.0 - age / 0.35);
        return position.add(right.scale(offset));
    }

    /**
     * Corrects the baked clock item's slight right/down visual bias relative to
     * its surface anchor. Shift it left and away from the player while keeping
     * the rune circles at their original shared center.
     */
    public static Vec3 clockModelPosition(Vec3 runeCenter, Direction facing) {
        if (runeCenter == null || facing == null) {
            return runeCenter;
        }
        Vec3 right = Vec3.atLowerCornerOf(facing.getOpposite().getClockWise().getNormal());
        Vec3 towardsPlayer = Vec3.atLowerCornerOf(facing.getNormal());
        return runeCenter
                .subtract(right.scale(CLOCK_CENTER_LEFT_OFFSET))
                .subtract(towardsPlayer.scale(CLOCK_CENTER_AWAY_OFFSET));
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
            int light,
            Object station) {
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
        double layoutScale = lapisSocketScale(station);
        // Move the whole clock + rune group together using EnchantStation's
        // exact failed-enchantment shake (0.02 blocks, 0.35 seconds, 60 rad/s).
        Vec3 visualPosition = denialShake(position, facing, clock);
        float hoverScale = hovered ? 1.10f : 1.0f;
        float baseBrightness = affordable
                ? (hovered ? 0.92f : 0.68f)
                : (hovered ? 0.98f : 0.88f);

        // Mirror the real lapis socket: the violet pad uses the same radius,
        // vertical lift and layout scale as EnchantStation's lapis circle.
        // The lapis-blue perimeter extends just beyond that pad.
        Object outerPalette = palette(affordable ? "LAPIS" : "RED");
        Object innerPalette = palette(affordable ? "VIOLET" : "RED");
        Vec3 violetCenter = visualPosition.add(0.0, SOCKET_CIRCLE_LIFT, 0.0);
        Vec3 blueCenter = visualPosition.add(0.0, OUTER_CIRCLE_LIFT, 0.0);

        if (background > 0.005f) {
            float brightness = baseBrightness * background;
            drawCircle(
                    pose,
                    buffers,
                    cameraPos,
                    blueCenter,
                    BLUE_PERIMETER_RADIUS * layoutScale * hoverScale,
                    clock * (hovered ? 0.78 : 0.30),
                    outerPalette,
                    brightness
            );
            drawCircle(
                    pose,
                    buffers,
                    cameraPos,
                    violetCenter,
                    VIOLET_SOCKET_RADIUS * layoutScale * hoverScale,
                    -clock * 0.52,
                    innerPalette,
                    brightness * 0.82f
            );

            // Keep the glow centred on the socket. Insufficient resources turn
            // the complete background red while leaving the gold clock unchanged.
            float glowRed = affordable ? 0.18f : 1.0f;
            float glowGreen = affordable ? 0.46f : 0.10f;
            float glowBlue = affordable ? 0.95f : 0.14f;
            drawGlow(
                    pose,
                    buffers,
                    camera,
                    visualPosition.add(0.0, 0.030 * layoutScale, 0.0),
                    0.15 * BLUE_GLOW_SCALE * layoutScale * hoverScale,
                    glowRed * brightness,
                    glowGreen * brightness,
                    glowBlue * brightness,
                    1.0f
            );

            if (flashAge >= 0.0 && flashAge < 0.65) {
                float flash = (float) (1.0 - flashAge / 0.65) * background;
                drawCircle(
                        pose,
                        buffers,
                        cameraPos,
                        violetCenter.add(0.0, 0.003, 0.0),
                        (BLUE_PERIMETER_RADIUS + 0.018 * BLUE_GLOW_SCALE * (1.0 - flash)) * layoutScale,
                        clock * 1.8,
                        outerPalette,
                        flash * 0.95f
                );
            }

            LocalPlayer player = Minecraft.getInstance().player;
            if (affordable && background > 0.45f && player != null
                    && player.tickCount % 7 == 0 && ENCHANT_FX_SPARKLE != null) {
                try {
                    ENCHANT_FX_SPARKLE.invoke(
                            null,
                            violetCenter.add(0.0, 0.025 * BLUE_GLOW_SCALE * layoutScale, 0.0),
                            0.09 * BLUE_GLOW_SCALE * layoutScale
                    );
                } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                    // Purely decorative.
                }
            }
        }

        // The gold clock is independent of the rune palette and keeps its intro/outro scale animation.
        if (clockScale > 0.005f && ITEM_PLACER_RENDER != null) {
            try {
                ITEM_PLACER_RENDER.invoke(
                        null,
                        pose,
                        buffers,
                        cameraPos,
                        new ItemStack(Items.CLOCK),
                        clockModelPosition(visualPosition, facing).add(0.0, 0.009, 0.0),
                        facing == null ? Direction.NORTH : facing,
                        0x5245524F4C4CL,
                        CLOCK_MODEL_SCALE * clockScale * hoverScale,
                        0.0f,
                        light
                );
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Rune and glow remain visible if the optional item-model bridge changes.
            }
        }
    }

    /**
     * Queue the regular hand-tap animation. HandsTapRequestMixin consumes this
     * from Hands.follow() and updates the same tapAt timer as Hands.tap().
     */
    public static void tapHand() {
        handTapRequested = true;
    }

    public static boolean consumeHandTapRequest() {
        if (!handTapRequested) {
            return false;
        }
        handTapRequested = false;
        return true;
    }

    public static void playRerollBurst(Vec3 position) {
        if (position != null && ENCHANT_FX_BURST != null) {
            try {
                ENCHANT_FX_BURST.invoke(null, position, 0.55);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // The actual reroll action does not depend on particles.
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
