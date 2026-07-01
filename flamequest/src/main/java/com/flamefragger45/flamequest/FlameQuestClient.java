package com.flamefragger45.flamequest;

import com.flamefragger45.flamequest.client.ClientQuestData;
import com.flamefragger45.flamequest.data.Quest;
import com.flamefragger45.flamequest.data.QuestProgress;
import com.flamefragger45.flamequest.network.NetworkHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.nbt.NbtCompound;

import java.util.*;

public class FlameQuestClient implements ClientModInitializer {

    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public void onInitializeClient() {

        // Receive quest sync packet from server
        ClientPlayNetworking.registerGlobalReceiver(NetworkHandler.SYNC_QUESTS_ID,
            (client, handler, buf, responseSender) -> {
                String questJson  = buf.readString(131072);
                NbtCompound progNbt = buf.readNbt();

                // Parse on network thread, then apply on render thread
                List<Quest> quests = GSON.fromJson(questJson,
                    new TypeToken<List<Quest>>(){}.getType());

                Map<String, QuestProgress> progressMap = new HashMap<>();
                if (progNbt != null) {
                    for (String key : progNbt.getKeys()) {
                        progressMap.put(key, QuestProgress.fromNbt(progNbt.getCompound(key)));
                    }
                }

                client.execute(() -> ClientQuestData.update(quests, progressMap));
            });
    }
}
