package com.flamefragger45.flamequest.item;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class QuestBookItem extends Item {

    public QuestBookItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            openScreen();
        }
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Environment(EnvType.CLIENT)
    private void openScreen() {
        com.flamefragger45.flamequest.client.screen.QuestBookScreen screen =
            new com.flamefragger45.flamequest.client.screen.QuestBookScreen(
                net.minecraft.text.Text.translatable("screen.flamequest.quest_book")
            );
        MinecraftClient.getInstance().setScreen(screen);
    }
}
