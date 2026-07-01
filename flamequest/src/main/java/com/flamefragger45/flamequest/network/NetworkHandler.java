package com.flamefragger45.flamequest.network;

import com.flamefragger45.flamequest.data.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.*;

public class NetworkHandler {

    // Packet IDs
    public static final Identifier SYNC_QUESTS_ID  = new Identifier("flamequest", "sync_quests");
    public static final Identifier CLAIM_REWARD_ID = new Identifier("flamequest", "claim_reward");

    private static final Gson GSON = new GsonBuilder().create();

    public static void register() {
        // Client -> Server: player wants to claim a quest reward
        ServerPlayNetworking.registerGlobalReceiver(CLAIM_REWARD_ID,
            (server, player, handler, buf, responseSender) -> {
                String questId = buf.readString(256);
                server.execute(() -> {
                    QuestManager.get(server).claimReward(player, questId);
                });
            });
    }

    /** Send quest definitions + player progress to client */
    public static void syncToClient(ServerPlayerEntity player) {
        try {
            // Serialise quest definitions as JSON
            String questJson = GSON.toJson(QuestLoader.QUESTS);

            // Serialise player progress as NBT
            QuestManager mgr = QuestManager.get(player.getServer());
            Map<String, QuestProgress> progressMap = mgr.getPlayerData(player.getUuid());
            NbtCompound progressNbt = new NbtCompound();
            for (Map.Entry<String, QuestProgress> e : progressMap.entrySet()) {
                progressNbt.put(e.getKey(), e.getValue().toNbt());
            }

            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(questJson, 131072); // 128k max
            buf.writeNbt(progressNbt);

            ServerPlayNetworking.send(player, SYNC_QUESTS_ID, buf);
        } catch (Exception e) {
            // Log but don't crash
        }
    }
}
