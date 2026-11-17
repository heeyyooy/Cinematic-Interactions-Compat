package ru.heeyyooy.cinematiccompat.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.bus.api.IEventBus;
import ru.heeyyooy.cinematiccompat.CinematicCompat;

public final class GoetyDarkAnvilBlockEntities {
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, CinematicCompat.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GoetyDarkAnvilBlockEntity>> DARK_ANVIL =
            BLOCK_ENTITIES.register("goety_dark_anvil", () -> BlockEntityType.Builder.of(
                    GoetyDarkAnvilBlockEntity::new,
                    block("dark_anvil"), block("chipped_dark_anvil"), block("damaged_dark_anvil")
            ).build(null));

    private GoetyDarkAnvilBlockEntities() {}

    public static void register(IEventBus modEventBus) {
        BLOCK_ENTITIES.register(modEventBus);
    }

    private static Block block(String path) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("goety", path));
    }
}
