package com.flamefragger45.flamequest.event;

import com.flamefragger45.flamequest.data.QuestManager;
import com.flamefragger45.flamequest.network.NetworkHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.item.ItemStack;

public class QuestEventHandler {

    private static int tickCounter = 0;

    public static void register() {

        // ── Kill tracking ──────────────────────────────────────────────────────
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player) {
                String entityId = Registries.ENTITY_TYPE.getId(killedEntity.getType()).toString();
                QuestManager.get(world.getServer()).onKill(player, entityId);
            }
        });

        // ── Inventory (collect) tracking — every 20 ticks (1 second) ──────────
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter % 20 == 0) {
                QuestManager mgr = QuestManager.get(server);
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    mgr.tickPlayer(player);
                }
            }
        });

        // ── Sync on player join ────────────────────────────────────────────────
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            server.execute(() -> NetworkHandler.syncToClient(handler.player));
        });
    }

    /** Called from the CraftingResultSlotMixin */
    public static void onItemCrafted(ServerPlayerEntity player, ItemStack stack) {
        if (stack.isEmpty()) return;
        String itemId = Registries.ITEM.getId(stack.getItem()).toString();
        QuestManager.get(player.getServer()).onCraft(player, itemId);
    }
}
