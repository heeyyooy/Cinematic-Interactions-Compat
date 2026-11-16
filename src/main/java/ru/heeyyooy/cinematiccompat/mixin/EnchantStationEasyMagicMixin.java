package ru.heeyyooy.cinematiccompat.mixin;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Easy Magic keeps its enchantment clues in its own menu field rather than
 * vanilla EnchantmentMenu.enchantClue. Use those clues and the same XP/lapis
 * requirements so valid Easy Magic offers are not marked as un-enchantable.
 */
@Pseudo
@Mixin(targets = "dev.dyslexic.cinematic.client.EnchantStation", remap = false)
public abstract class EnchantStationEasyMagicMixin {
    @Unique
    private static final String CINEMATICCOMPAT$EASY_MAGIC_MENU =
            "fuzs.easymagic.world.inventory.ModEnchantmentMenu";

    @Inject(method = "affordable", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$checkEasyMagicAffordability(
            int index,
            CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        EnchantmentMenu menu = player != null && player.containerMenu instanceof EnchantmentMenu enchantmentMenu
                ? enchantmentMenu
                : null;

        if (!cinematiccompat$isEasyMagicMenu(menu)) {
            return;
        }

        cir.setReturnValue(cinematiccompat$canEnchant(menu, player, index));
    }

    @Inject(method = "offerTooltip", at = @At("HEAD"), cancellable = true, remap = false)
    private void cinematiccompat$useEasyMagicTooltip(
            EnchantmentMenu menu,
            int index,
            CallbackInfoReturnable<List<Component>> cir) {
        if (!cinematiccompat$isEasyMagicMenu(menu)
                || index < 0
                || index >= menu.costs.length
                || menu.costs[index] <= 0) {
            return;
        }

        LocalPlayer player = Minecraft.getInstance().player;
        List<Component> lines = new ArrayList<>();

        for (EnchantmentInstance clue : cinematiccompat$getEasyMagicClues(menu, index)) {
            MutableComponent enchantmentText = Component.empty()
                    .append(Enchantment.getFullname(clue.enchantment, clue.level))
                    .withStyle(ChatFormatting.GRAY);
            lines.add(Component.translatable("container.enchant.clue", enchantmentText));
        }

        if (player == null || player.getAbilities().instabuild) {
            cir.setReturnValue(lines);
            return;
        }

        if (!lines.isEmpty()) {
            lines.add(Component.empty());
        }

        int required = index + 1;
        if (player.experienceLevel < menu.costs[index]) {
            lines.add(Component.translatable(
                    "container.enchant.level.requirement",
                    menu.costs[index]).withStyle(ChatFormatting.RED));
        } else {
            MutableComponent lapisLine = required == 1
                    ? Component.translatable("container.enchant.lapis.one")
                    : Component.translatable("container.enchant.lapis.many", required);
            MutableComponent levelLine = required == 1
                    ? Component.translatable("container.enchant.level.one")
                    : Component.translatable("container.enchant.level.many", required);

            lines.add(lapisLine.withStyle(menu.getSlot(1).getItem().getCount() >= required
                    ? ChatFormatting.GRAY
                    : ChatFormatting.RED));
            lines.add(levelLine.withStyle(player.experienceLevel >= required
                    ? ChatFormatting.GRAY
                    : ChatFormatting.RED));
        }

        if (cinematiccompat$canEnchant(menu, player, index)) {
            lines.add(Component.translatable("cinematic.enchanting.touch")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }

        cir.setReturnValue(lines);
    }

    @Unique
    private static boolean cinematiccompat$isEasyMagicMenu(EnchantmentMenu menu) {
        if (menu == null) {
            return false;
        }

        for (Class<?> type = menu.getClass(); type != null; type = type.getSuperclass()) {
            if (CINEMATICCOMPAT$EASY_MAGIC_MENU.equals(type.getName())) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private static List<EnchantmentInstance> cinematiccompat$getEasyMagicClues(
            EnchantmentMenu menu,
            int index) {
        try {
            Field cluesField = menu.getClass().getField("clues");
            Object value = cluesField.get(menu);
            if (!(value instanceof List<?> allClues)
                    || index < 0
                    || index >= allClues.size()
                    || !(allClues.get(index) instanceof List<?> offerClues)) {
                return List.of();
            }

            List<EnchantmentInstance> result = new ArrayList<>(offerClues.size());
            for (Object clue : offerClues) {
                if (clue instanceof EnchantmentInstance enchantmentInstance) {
                    result.add(enchantmentInstance);
                }
            }
            return result;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return List.of();
        }
    }

    @Unique
    private static boolean cinematiccompat$canEnchant(
            EnchantmentMenu menu,
            LocalPlayer player,
            int index) {
        if (menu == null || player == null || index < 0
                || index >= menu.costs.length || menu.costs[index] <= 0) {
            return false;
        }

        if (player.getAbilities().instabuild) {
            return true;
        }

        int required = index + 1;
        return player.experienceLevel >= menu.costs[index]
                && player.experienceLevel >= required
                && menu.getSlot(1).getItem().getCount() >= required;
    }
}
