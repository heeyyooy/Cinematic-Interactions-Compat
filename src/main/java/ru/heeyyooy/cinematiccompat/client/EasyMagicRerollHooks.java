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
 * All Easy Magic and Cinematic Interactions implementation references are
 * resolved by name so the addon can still load without those client classes.
 */
public final class EasyMagicRerollHooks {
    private static final Class<?> MENU_CLASS;
    private static final Method CAN_ENCHANT_ITEM;
    private static final Method CAN_USE_REROLL;

    private static final Method PICKING_MOUSE_GUI;
    private static final Method PICKING_TO_GUI;
    private static final Field STATION_AIM;

    private static final Method ITEM_PLACER_RENDER;
    private static final Method ENCHANT_FX_CIRCLE;
    private static final Method ENCHANT_FX_BILLBOARD;
    private static final Method ENCHANT_FX_SPARKLE;
    private static final Method ENCHANT_FX_BURST;
    private static final Field ENCHANT_FX_GLOW;
    private static final Class<?> CIRCLE_PALETTE;

    private static final Method SCENE_CLOCK;

    static {
        Class<?> menuClass = null;
        Method canEnchantItem = null;
        Method canUseReroll = null;
        Method mouseGui = null;
        Method toGui = null;
        Field stationAim = null;
        Method itemPlacerRender = null;
        Method enchantFxCircle = null;
        Method enchantFxBillboard = null;
        Method enchantFxSparkle = null;
        Method enchantFxBurst = null;
        Field enchantFxGlow = null;
        Class<?> circlePalette = null;
        Method sceneClock = null;

        ClassLoader loader = EasyMagicRerollHooks.class.getClassLoader();
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
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Cursor and hand integration will be disabled if Picking is unavailable.
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
            // The item layer remains usable even if an optional decoration method changes.
        }

        try {
            Class<?> sceneClass = Class.forName("dev.dyslexic.cinematic.client.Scene", false, loader);
            sceneClock = sceneClass.getMethod("clock");
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The local player tick count is used as a fallback for animation timing.
        }

        MENU_CLASS = menuClass;
        CAN_ENCHANT_ITEM = canEnchantItem;
        CAN_USE_REROLL = canUseReroll;
        PICKING_MOUSE_GUI = mouseGui;
        PICKING_TO_GUI = toGui;
        STATION_AIM = stationAim;
        ITEM_PLACER_RENDER = itemPlacerRender;
        ENCHANT_FX_CIRCLE = enchantFxCircle;
        ENCHANT_FX_BILLBOARD = enchantFxBillboard;
        ENCHANT_FX_SPARKLE = enchantFxSparkle;
        ENCHANT_FX_BURST = enchantFxBurst;
        ENCHANT_FX_GLOW = enchantFxGlow;
        CIRCLE_PALETTE = circlePalette;
        SCENE_CLOCK = sceneClock;
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
        if (menu == null || CAN_ENCHANT_ITEM == null) {
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
        if (menu == null || player == null || CAN_USE_REROLL == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(CAN_USE_REROLL.invoke(menu, player));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    /**
     * Mirrors ModEnchantmentScreen's callback. The mod validates the action,
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
            // Reroll remains clickable even if the hand aim cannot be moved.
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

    /**
     * A small physical reroll token: a dropped clock model, surrounded by the
     * same pixel-rune and glow effects used by the enchantment offers.
     */
    public static void renderButton(
            PoseStack pose,
            MultiBufferSource buffers,
            Camera camera,
            Vec3 position,
            boolean hovered,
            boolean affordable,
            double flashAge,
            int light) {
        if (pose == null || buffers == null || camera == null || position == null) {
            return;
        }

        Vec3 cameraPos = camera.getPosition();
        double clock = clock();
        float scale = hovered ? 1.12f : 1.0f;
        float brightness = affordable ? (hovered ? 0.92f : 0.68f) : (hovered ? 0.60f : 0.40f);
        Object outerPalette = palette(affordable ? "VIOLET" : "RED");
        Object innerPalette = palette(affordable ? "LAPIS" : "RED");
        Vec3 center = position.add(0.0, 0.006, 0.0);

        drawCircle(pose, buffers, cameraPos, center,
                0.155 * scale, clock * (hovered ? 0.85 : 0.34), outerPalette, brightness);
        drawCircle(pose, buffers, cameraPos, center.add(0.0, 0.001, 0.0),
                0.092 * scale, -clock * 0.58, innerPalette, brightness * 0.72f);
        drawGlow(pose, buffers, camera, center.add(0.0, 0.018, 0.0),
                0.18 * scale, affordable ? 0.55f : 0.36f,
                affordable ? 0.20f : 0.08f, affordable ? 0.95f : 0.22f,
                brightness * (hovered ? 0.95f : 0.58f));

        // Clock's gold dial makes the central object read as a physical button.
        if (ITEM_PLACER_RENDER != null) {
            try {
                ITEM_PLACER_RENDER.invoke(
                        null,
                        pose,
                        buffers,
                        cameraPos,
                        new ItemStack(Items.CLOCK),
                        position.add(0.0, 0.009, 0.0),
                        Direction.NORTH,
                        0x5245524F4C4CL,
                        0.78f * scale,
                        0.0f,
                        light
                );
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // The rune and glow remain visible if the item renderer bridge changes.
            }
        }

        if (flashAge >= 0.0 && flashAge < 0.65) {
            float flash = (float) (1.0 - flashAge / 0.65);
            drawCircle(pose, buffers, cameraPos, center.add(0.0, 0.003, 0.0),
                    0.18 + 0.08 * (1.0 - flash), clock * 1.8, outerPalette, flash * 0.95f);
        }

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.tickCount % 7 == 0 && ENCHANT_FX_SPARKLE != null) {
            try {
                ENCHANT_FX_SPARKLE.invoke(null, center.add(0.0, 0.025, 0.0), 0.11);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
                // Purely decorative.
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
        try {
            Class<?> handsClass = Class.forName(
                    "dev.dyslexic.cinematic.client.Hands",
                    false,
                    EasyMagicRerollHooks.class.getClassLoader());
            handsClass.getMethod("tap").invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // The action still works if the optional hand animation API changes.
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
            PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 center,
            double radius, double angle, Object palette, float brightness) {
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
            PoseStack pose, MultiBufferSource buffers, Camera camera, Vec3 center, double radius,
            float red, float green, float blue, float brightness) {
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
