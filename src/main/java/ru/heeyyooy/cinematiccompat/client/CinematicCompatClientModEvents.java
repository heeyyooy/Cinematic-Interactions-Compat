package ru.heeyyooy.cinematiccompat.client;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import ru.heeyyooy.cinematiccompat.CinematicCompat;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntities;

@EventBusSubscriber(
        modid = CinematicCompat.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class CinematicCompatClientModEvents {
    private CinematicCompatClientModEvents() {
    }

    @SubscribeEvent
    public static void registerBlockEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        if (!ModList.get().isLoaded("goety")) {
            return;
        }

        event.registerBlockEntityRenderer(
                GoetyDarkAnvilBlockEntities.DARK_ANVIL.get(),
                GoetyDarkAnvilRenderer::new
        );
    }
}
