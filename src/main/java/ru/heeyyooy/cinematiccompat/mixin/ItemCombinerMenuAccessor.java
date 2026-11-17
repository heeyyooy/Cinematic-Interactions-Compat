package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.ItemCombinerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemCombinerMenu.class)
public interface ItemCombinerMenuAccessor {
    @Accessor("inputSlots")
    @Mutable
    void cinematiccompat$setInputSlots(Container inputSlots);

    @Accessor("access")
    ContainerLevelAccess cinematiccompat$getAccess();

    @Accessor("access")
    @Mutable
    void cinematiccompat$setAccess(ContainerLevelAccess access);
}
