package ru.heeyyooy.cinematiccompat;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import ru.heeyyooy.cinematiccompat.blockentity.GoetyDarkAnvilBlockEntities;

@Mod(CinematicCompat.MOD_ID)
public final class CinematicCompat {
    public static final String MOD_ID = "cinematiccompat";
    private static final Logger LOGGER = LogUtils.getLogger();

    public CinematicCompat(IEventBus modEventBus) {
        String originalVersion = ModList.get().getModContainerById("cinematic")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("missing");
        if (ModList.get().isLoaded("goety")) {
            GoetyDarkAnvilBlockEntities.register(modEventBus);
        }
        LOGGER.info("[CinematicCompat] Addon loaded; Cinematic Interactions version detected: {}", originalVersion);
    }
}
