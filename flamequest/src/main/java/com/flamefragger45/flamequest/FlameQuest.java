package com.flamefragger45.flamequest;

import com.flamefragger45.flamequest.data.QuestLoader;
import com.flamefragger45.flamequest.event.QuestEventHandler;
import com.flamefragger45.flamequest.item.ModItems;
import com.flamefragger45.flamequest.network.NetworkHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resource.ResourceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlameQuest implements ModInitializer {

    public static final String MOD_ID = "flamequest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[FlameQuest] Initialising...");

        // Register items
        ModItems.register();

        // Register server network handlers
        NetworkHandler.register();

        // Register game events (kills, ticks, joins)
        QuestEventHandler.register();

        // Register quest data loader (reads JSON from data/flamequest/quests/)
        ResourceManagerHelper.get(ResourceType.SERVER_DATA)
            .registerReloadListener(new QuestLoader());

        LOGGER.info("[FlameQuest] Ready!");
    }
}
