package ru.heeyyooy.cinematiccompat.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
 * Compatibility for the Carved Wood crimson chiseled bookshelf.
 *
 * Important: the occupied texture's book-art rectangles and the block model's
 * UV rectangles are not interchangeable. These bounds describe the actual
 * individual book artwork in chiseled_crimson_bookshelf_occupied.png.
 */
public final class CarvedBookshelfCompatHooks {
    public static final int NOT_HANDLED = Integer.MIN_VALUE;

    private static final ResourceLocation CRIMSON_SHELF =
            ResourceLocation.fromNamespaceAndPath("carved_wood", "chiseled_crimson_bookshelf");
    private static final String CRIMSON_TEXTURE =
            "carved_wood:block/chiseled_crimson_bookshelf_occupied";

    private static final int[][] VANILLA_SPINES = {
            {1, 2, 5, 7}, {6, 1, 10, 7}, {11, 1, 15, 7},
            {1, 9, 5, 15}, {6, 10, 10, 15}, {11, 9, 15, 15}
    };

    /*
     * Actual coloured book bounds in Carved Wood's crimson occupied texture.
     * Order follows SLOT_OCCUPIED_PROPERTIES: top-left, top-mid, top-right,
     * bottom-left, bottom-mid, bottom-right.
     *
     * These are deliberately different from the large UV rectangles used by
     * the six flat book planes in the block's JSON models.
     */
    private static final int[][] CRIMSON_SPINES = {
            {4, 1, 6, 5},
            {11, 1, 15, 5},
            {2, 7, 5, 11},
            {8, 7, 14, 11},
            {1, 12, 8, 16},
            {11, 12, 15, 16}
    };

    private record SpineRect(int u0, int v0, int u1, int v1) {
        int width() {
            return u1 - u0;
        }

        int height() {
            return v1 - v0;
        }

        boolean horizontal() {
            return width() > height();
        }
    }

    private record ShelfContext(SpineRect spine) {
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
        return CRIMSON_SHELF.equals(BuiltInRegistries.BLOCK.getKey(block))
                && block instanceof ChiseledBookShelfBlock;
    }

    private static SpineRect rect(int slot) {
        if (slot < 0 || slot >= CRIMSON_SPINES.length) {
            return null;
        }
        int[] values = CRIMSON_SPINES[slot];
        return new SpineRect(values[0], values[1], values[2], values[3]);
    }

    private static ShelfContext context(BlockPos pos, int slot) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null
                || !isSupportedShelf(minecraft.level.getBlockState(pos))) {
            CURRENT.remove();
            return null;
        }

        SpineRect rect = rect(slot);
        if (rect == null) {
            CURRENT.remove();
            return null;
        }

        ShelfContext context = new ShelfContext(rect);
        CURRENT.set(context);
        return context;
    }

    public static int[][] spinesFor(BlockPos pos) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || pos == null
                || !isSupportedShelf(minecraft.level.getBlockState(pos))) {
            return VANILLA_SPINES;
        }

        int[][] copy = new int[CRIMSON_SPINES.length][];
        for (int i = 0; i < CRIMSON_SPINES.length; i++) {
            copy[i] = CRIMSON_SPINES[i].clone();
        }
        return copy;
    }

    public static int[][] spinesForCurrentScene() {
        return spinesFor(CinematicCompatClientHooks.currentScenePos());
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

        for (int i = 0; i < CRIMSON_SPINES.length; i++) {
            int[] r = CRIMSON_SPINES[i];
            if (u >= r[0] - 0.001 && u <= r[2] + 0.001
                    && v >= r[1] - 0.001 && v <= r[3] + 0.001) {
                context(pos, i);
                return i;
            }
        }
        return -1;
    }

    /** Position of the centre of the actual spine artwork on the shelf face. */
    public static Vec3 spine(BlockPos pos, Direction facing, int slot) {
        ShelfContext context = context(pos, slot);
        if (context == null) {
            return null;
        }

        SpineRect r = context.spine();
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
     * Creates Cinematic Interactions' ShelfBooks.Pose by reflection so this
     * compatibility add-on does not require a compile-time dependency on it.
     */
    public static Object inSlot(BlockPos pos, Direction facing, int slot, double out) {
        ShelfContext context = context(pos, slot);
        if (context == null) {
            return null;
        }

        SpineRect r = context.spine();
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

            return poseConstructor().newInstance(origin, turn, size, 1.0);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    /**
     * Uses only the individual book's pixels for its cover and spine. The
     * previous implementation sampled the whole model UV tile, making the
     * hovering book display a large fragment of the bookshelf texture.
     */
    public static Object look(int slot) {
        ShelfContext context = CURRENT.get();
        if (context == null || slot < 0 || slot >= CRIMSON_SPINES.length) {
            return null;
        }

        SpineRect r = context.spine();
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
            sv0 = (r.v0() + 1.3f) / 16.0f;
            su1 = u1;
            sv1 = (r.v0() + 1.7f) / 16.0f;
        } else {
            su0 = (r.u0() + 1.3f) / 16.0f;
            sv0 = v0;
            su1 = (r.u0() + 1.7f) / 16.0f;
            sv1 = v1;
        }

        try {
            return lookConstructor().newInstance(CRIMSON_TEXTURE,
                    u0, v0, u1, v1, su0, sv0, su1, sv1);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
            return null;
        }
    }

    /** Resolve a resource location with its real namespace, rather than minecraft by default. */
    public static TextureAtlasSprite sprite(String texture) {
        ResourceLocation location = ResourceLocation.tryParse(texture);
        if (location == null) {
            location = ResourceLocation.withDefaultNamespace(texture);
        }
        return Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(location);
    }

    public static boolean isCurrentBookHorizontal() {
        ShelfContext context = CURRENT.get();
        return context != null && context.spine().horizontal();
    }

    public static float[] rotateUvs(float[] original) {
        return original;
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

    /** Extends Cinematic Interactions' hidden-slot model wrapper to this shelf only. */
    public static void wrapModels(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        IdentityHashMap<BakedModel, BakedModel> wrapped = new IdentityHashMap<>();

        for (BlockState state : BuiltInRegistries.BLOCK.get(CRIMSON_SHELF).getStateDefinition().getPossibleStates()) {
            ModelResourceLocation key = BlockModelShaper.stateToModelLocation(state);
            BakedModel model = models.get(key);
            if (model != null) {
                models.put(key, wrapped.computeIfAbsent(model, CarvedBookshelfEmptiedModel::new));
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
