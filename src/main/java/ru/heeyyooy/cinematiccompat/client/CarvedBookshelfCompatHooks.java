package ru.heeyyooy.cinematiccompat.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.joml.Matrix3f;
import org.joml.Quaternionf;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Runtime compatibility for Carved Wood and its Every Compat variants.
 *
 * Carved Wood's slot models contain the authoritative front-face rectangles and
 * UV regions. The tables below are those model rectangles converted from model
 * coordinates (bottom-left origin) to the top-left coordinates used by
 * Cinematic Interactions' ShelfBooks.SPINES.
 */
public final class CarvedBookshelfCompatHooks {
    public static final int NOT_HANDLED = Integer.MIN_VALUE;

    private static final int[][] VANILLA_SPINES = {
            {1, 2, 5, 7}, {6, 1, 10, 7}, {11, 1, 15, 7},
            {1, 9, 5, 15}, {6, 10, 10, 15}, {11, 9, 15, 15}
    };

    // Slot order is the BlockState property order: top_left, top_mid, top_right,
    // bottom_left, bottom_mid, bottom_right. Each rectangle is u0, v0, u1, v1.
    private static final int[][][] LAYOUTS = {
            { // Type 1
                    {0, 0, 6, 8}, {6, 0, 11, 8}, {11, 0, 16, 8},
                    {0, 8, 6, 16}, {6, 8, 11, 16}, {11, 8, 16, 16}
            },
            { // Type 2
                    {0, 0, 8, 8}, {8, 0, 12, 8}, {12, 0, 16, 8},
                    {0, 8, 4, 16}, {4, 8, 8, 16}, {8, 8, 16, 16}
            },
            { // Type 3
                    {0, 0, 8, 5}, {8, 0, 16, 5}, {0, 5, 8, 11},
                    {8, 5, 16, 11}, {0, 11, 8, 16}, {8, 11, 16, 16}
            },
            { // Type 4
                    {0, 0, 10, 5}, {10, 0, 16, 5}, {0, 5, 6, 11},
                    {6, 5, 16, 11}, {0, 11, 10, 16}, {10, 11, 16, 16}
            },
            { // Type 5
                    {0, 0, 8, 4}, {0, 4, 8, 8}, {8, 0, 16, 8},
                    {0, 8, 8, 16}, {8, 8, 16, 12}, {8, 12, 16, 16}
            },
            { // Type 6
                    {0, 0, 8, 6}, {8, 0, 16, 6}, {0, 6, 8, 11},
                    {8, 6, 16, 11}, {0, 11, 8, 16}, {8, 11, 16, 16}
            },
            { // Type 7
                    {0, 0, 5, 5}, {5, 0, 11, 11}, {11, 0, 16, 5},
                    {0, 5, 5, 16}, {5, 11, 11, 16}, {11, 5, 16, 16}
            },
            { // Type 8
                    {0, 0, 6, 10}, {6, 0, 11, 9}, {11, 0, 16, 7},
                    {0, 10, 6, 16}, {6, 9, 11, 16}, {11, 7, 16, 16}
            }
    };

    private record SlotRect(int u0, int v0, int u1, int v1) {
        int width() {
            return u1 - u0;
        }

        int height() {
            return v1 - v0;
        }

        boolean horizontal() {
            return width() > height();
        }

        int[] asArray() {
            return new int[]{u0, v0, u1, v1};
        }
    }

    private record ShelfContext(ResourceLocation blockId, SlotRect rect, String texture) {
    }

    private static final ThreadLocal<ShelfContext> CURRENT = new ThreadLocal<>();
    private static final ModelProperty<Integer> EMPTIED_SLOTS = new ModelProperty<>();

    private static volatile Field hiddenSlotsField;
    private static volatile Method customShelfMethod;
    private static volatile Constructor<?> poseConstructor;
    private static volatile Constructor<?> lookConstructor;

    private CarvedBookshelfCompatHooks() {
    }

    public static boolean isSupportedShelf(BlockState state) {
        return state != null && isSupportedBlock(state.getBlock());
    }

    private static boolean isSupportedBlock(Block block) {
        if (!(block instanceof ChiseledBookShelfBlock)) {
            return false;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        if (id == null) {
            return false;
        }

        String path = id.getPath();
        boolean bookshelfPath = path.contains("chiseled_") && path.endsWith("_bookshelf");
        return bookshelfPath && (id.getNamespace().equals("carved_wood")
                || id.getNamespace().equals("everycomp") && path.startsWith("crwo/"));
    }

    private static int layoutType(Block block) {
        for (Class<?> type = block.getClass(); type != null; type = type.getSuperclass()) {
            String name = type.getSimpleName();
            String marker = "CWChiseledBookShelfBlockType";
            int index = name.lastIndexOf(marker);
            if (index >= 0 && index + marker.length() < name.length()) {
                char digit = name.charAt(index + marker.length());
                if (digit >= '1' && digit <= '8') {
                    return digit - '0';
                }
            }
        }
        // Every Compat creates its Carved Wood variants with Type1.
        return 1;
    }

    private static SlotRect rect(BlockState state, int slot) {
        if (!isSupportedShelf(state) || slot < 0 || slot >= 6) {
            return null;
        }
        int type = layoutType(state.getBlock());
        int[] values = LAYOUTS[type - 1][slot];
        return new SlotRect(values[0], values[1], values[2], values[3]);
    }

    private static ResourceLocation occupiedTexture(ResourceLocation blockId) {
        String path = "block/" + blockId.getPath() + "_occupied";
        if (blockId.getNamespace().equals("everycomp")) {
            path += "_m";
        }
        return ResourceLocation.fromNamespaceAndPath(blockId.getNamespace(), path);
    }

    private static ShelfContext context(BlockPos pos, int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) {
            CURRENT.remove();
            return null;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        SlotRect rect = rect(state, slot);
        if (rect == null) {
            CURRENT.remove();
            return null;
        }

        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        ShelfContext context = new ShelfContext(id, rect, occupiedTexture(id).toString());
        CURRENT.set(context);
        return context;
    }

    public static int[][] spinesFor(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) {
            return VANILLA_SPINES;
        }
        return spinesFor(minecraft.level.getBlockState(pos));
    }

    public static int[][] spinesForCurrentScene() {
        return spinesFor(CinematicCompatClientHooks.currentScenePos());
    }

    private static int[][] spinesFor(BlockState state) {
        if (!isSupportedShelf(state)) {
            return VANILLA_SPINES;
        }

        int[][] source = LAYOUTS[layoutType(state.getBlock()) - 1];
        int[][] copy = new int[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    public static int slotAt(BlockPos pos, Direction facing, Vec3 point) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null) {
            return NOT_HANDLED;
        }

        BlockState state = minecraft.level.getBlockState(pos);
        if (!isSupportedShelf(state)) {
            return NOT_HANDLED;
        }

        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        Vec3 relative = point.subtract(Vec3.atCenterOf(pos).add(normal.scale(0.5)));
        double u = 8.0 + relative.dot(right) * 16.0;
        double v = 8.0 - relative.y * 16.0;

        if (Math.abs(relative.dot(normal)) > 0.05 || u < 0.0 || u > 16.0 || v < 0.0 || v > 16.0) {
            return -1;
        }

        int[][] slots = LAYOUTS[layoutType(state.getBlock()) - 1];
        for (int i = 0; i < slots.length; i++) {
            int[] r = slots[i];
            if (u >= r[0] - 0.001 && u <= r[2] + 0.001
                    && v >= r[1] - 0.001 && v <= r[3] + 0.001) {
                return i;
            }
        }
        return -1;
    }

    public static Vec3 spine(BlockPos pos, Direction facing, int slot) {
        ShelfContext context = context(pos, slot);
        if (context == null) {
            return null;
        }

        SlotRect r = context.rect();
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        double u = (r.u0() + r.u1()) / 2.0;
        double v = (r.v0() + r.v1()) / 2.0;

        return Vec3.atCenterOf(pos)
                .add(normal.scale(0.504))
                .add(right.scale(u / 16.0 - 0.5))
                .add(0.0, 0.5 - v / 16.0, 0.0);
    }

    /**
     * Builds Cinematic Interactions' private ShelfBooks.Pose without linking
     * against the optional target mod at compile time.
     */
    public static Object inSlot(BlockPos pos, Direction facing, int slot, double out) {
        ShelfContext context = context(pos, slot);
        if (context == null) {
            return null;
        }

        SlotRect r = context.rect();
        boolean horizontal = r.horizontal();

        double deep = staticDouble("dev.dyslexic.cinematic.client.OpenBook", "DEEP", 0.421);
        double tall = staticDouble("dev.dyslexic.cinematic.client.OpenBook", "TALL", 0.428);
        double thick = staticDouble("dev.dyslexic.cinematic.client.OpenBook", "THICK", 0.056);
        double stack = staticDouble("dev.dyslexic.cinematic.client.OpenBook", "STACK", 0.032);
        double depth = staticDouble("dev.dyslexic.cinematic.client.ShelfBooks", "DEPTH", 0.32);

        double width = r.width() / 16.0;
        double height = r.height() / 16.0;
        Vec3 size = horizontal
                ? new Vec3(depth / deep, width / tall, height / thick)
                : new Vec3(depth / deep, height / tall, width / thick);

        Vec3 side = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        Vec3 up = new Vec3(0.0, 1.0, 0.0);

        // A horizontal book is laid on its side: its long axis follows the
        // shelf horizontally, while its spine thickness follows world-up.
        Vec3 bookUp = horizontal ? right.scale(-1.0) : up;
        Vec3 bookRight = horizontal ? up : right;

        Vec3 origin = spine(pos, facing, slot)
                .add(side.scale(out))
                .subtract(bookRight.scale(stack * 0.5 * size.z));

        try {
            Quaternionf turn = new Matrix3f(
                    (float) -side.x, (float) -side.y, (float) -side.z,
                    (float) bookUp.x, (float) bookUp.y, (float) bookUp.z,
                    (float) bookRight.x, (float) bookRight.y, (float) bookRight.z
            ).getNormalizedRotation(new Quaternionf());

            Constructor<?> constructor = poseConstructor();
            return constructor.newInstance(origin, turn, size, 1.0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    /**
     * Reuses the exact occupied-texture UV rectangle from the Carved Wood
     * slot model, and extracts the narrow spine edge from that rectangle.
     */
    public static Object look(int slot) {
        ShelfContext context = CURRENT.get();
        if (context == null || slot < 0 || slot >= 6) {
            return null;
        }

        SlotRect r = context.rect();
        float u0 = r.u0() / 16.0f;
        float v0 = r.v0() / 16.0f;
        float u1 = r.u1() / 16.0f;
        float v1 = r.v1() / 16.0f;

        float su0;
        float sv0;
        float su1;
        float sv1;
        if (r.horizontal()) {
            su0 = u0;
            sv0 = (r.v0() + 0.3f) / 16.0f;
            su1 = u1;
            sv1 = (r.v0() + 0.7f) / 16.0f;
        } else {
            su0 = (r.u0() + 0.3f) / 16.0f;
            sv0 = v0;
            su1 = (r.u0() + 0.7f) / 16.0f;
            sv1 = v1;
        }

        try {
            Constructor<?> constructor = lookConstructor();
            return constructor.newInstance(context.texture(), u0, v0, u1, v1, su0, sv0, su1, sv1);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    public static boolean isCurrentBookHorizontal() {
        ShelfContext context = CURRENT.get();
        return context != null && context.rect().horizontal();
    }

    /**
     * Rotates the four UV corners by 90 degrees. The geometry mixin uses the
     * same rotation for the board and spine so the texture follows horizontal
     * books instead of being stretched across the wrong axis.
     */
    public static float[] rotateUvs(float[] original) {
        if (!isCurrentBookHorizontal() || original == null || original.length != 8) {
            return original;
        }
        return new float[]{
                original[6], original[7],
                original[0], original[1],
                original[2], original[3],
                original[4], original[5]
        };
    }

    public static void clearContext() {
        CURRENT.remove();
    }

    public static boolean shelfCheck(BlockState state, Block expected) {
        return state.is(expected) || expected == Blocks.CHISELED_BOOKSHELF && isSupportedShelf(state);
    }

    private static Constructor<?> poseConstructor() throws ReflectiveOperationException {
        if (poseConstructor == null) {
            Class<?> type = Class.forName("dev.dyslexic.cinematic.client.ShelfBooks$Pose", false,
                    CarvedBookshelfCompatHooks.class.getClassLoader());
            Constructor<?> constructor = type.getDeclaredConstructor(Vec3.class, Quaternionf.class, Vec3.class, double.class);
            constructor.setAccessible(true);
            poseConstructor = constructor;
        }
        return poseConstructor;
    }

    private static Constructor<?> lookConstructor() throws ReflectiveOperationException {
        if (lookConstructor == null) {
            Class<?> type = Class.forName("dev.dyslexic.cinematic.client.OpenBook$Look", false,
                    CarvedBookshelfCompatHooks.class.getClassLoader());
            Constructor<?> constructor = type.getDeclaredConstructor(
                    String.class, float.class, float.class, float.class, float.class,
                    float.class, float.class, float.class, float.class);
            constructor.setAccessible(true);
            lookConstructor = constructor;
        }
        return lookConstructor;
    }

    private static double staticDouble(String className, String fieldName, double fallback) {
        try {
            Class<?> type = Class.forName(className, false, CarvedBookshelfCompatHooks.class.getClassLoader());
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            return ((Number) field.get(null)).doubleValue();
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return fallback;
        }
    }

    /**
     * Called after Cinematic Interactions wraps the vanilla shelf models.
     * This adds equivalent wrappers for Carved Wood and Every Compat block
     * states so the static spine disappears while its animated book is out.
     */
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        IdentityHashMap<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!isSupportedBlock(block)) {
                continue;
            }

            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
                BakedModel model = models.get(key);
                if (model != null) {
                    models.put(key, wrapped.computeIfAbsent(model, CarvedBookshelfEmptiedModel::new));
                }
            }
        }
    }

    private static Integer hiddenMask(BlockPos pos) {
        try {
            Field field = hiddenSlotsField;
            if (field == null) {
                Class<?> type = Class.forName("dev.dyslexic.cinematic.client.ShelfBooks", false,
                        CarvedBookshelfCompatHooks.class.getClassLoader());
                field = type.getDeclaredField("HIDDEN");
                field.setAccessible(true);
                hiddenSlotsField = field;
            }

            @SuppressWarnings("unchecked")
            Map<BlockPos, Integer> hidden = (Map<BlockPos, Integer>) field.get(null);
            Integer mask = hidden.get(pos);
            return customShelfLayout() ? 63 : mask;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    private static boolean customShelfLayout() {
        try {
            Method method = customShelfMethod;
            if (method == null) {
                Class<?> type = Class.forName("dev.dyslexic.cinematic.client.SpatialLayouts", false,
                        CarvedBookshelfCompatHooks.class.getClassLoader());
                method = type.getDeclaredMethod("customShelf");
                method.setAccessible(true);
                customShelfMethod = method;
            }
            return Boolean.TRUE.equals(method.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return false;
        }
    }

    private static final class CarvedBookshelfEmptiedModel extends BakedModelWrapper<BakedModel> {
        private CarvedBookshelfEmptiedModel(BakedModel original) {
            super(original);
        }

        @Override
        public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData data) {
            ModelData own = originalModel.getModelData(level, pos, state, data);
            if (!isSupportedShelf(state)) {
                return own;
            }
            Integer mask = hiddenMask(pos);
            return mask == null ? own : own.derive().with(EMPTIED_SLOTS, mask).build();
        }

        @Override
        public java.util.List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource random,
                                                  ModelData data, RenderType renderType) {
            Integer mask = data == null ? null : data.get(EMPTIED_SLOTS);
            if (mask != null && isSupportedShelf(state)) {
                for (int i = 0; i < 6; i++) {
                    if ((mask & (1 << i)) != 0) {
                        state = state.setValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(i), false);
                    }
                }
            }
            return originalModel.getQuads(state, side, random, data, renderType);
        }
    }
}
