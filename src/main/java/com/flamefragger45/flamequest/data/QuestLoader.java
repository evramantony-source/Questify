package com.flamefragger45.flamequest.data;

import com.flamefragger45.flamequest.FlameQuest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class QuestLoader implements SimpleSynchronousResourceReloadListener {

    public static final Identifier ID = new Identifier("flamequest", "quest_loader");
    public static final List<Quest> QUESTS = new ArrayList<>();
    private static final Gson GSON = new GsonBuilder().create();

    @Override
    public Identifier getFabricId() { return ID; }

    @Override
    public void reload(ResourceManager manager) {
        QUESTS.clear();
        Map<Identifier, net.minecraft.resource.Resource> resources =
            manager.findResources("quests", id -> id.getPath().endsWith(".json"));

        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : resources.entrySet()) {
            if (!entry.getKey().getNamespace().equals("flamequest")) continue;
            try (Reader reader = new InputStreamReader(
                    entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                Quest quest = GSON.fromJson(reader, Quest.class);
                if (quest != null && quest.getId() != null) {
                    QUESTS.add(quest);
                    FlameQuest.LOGGER.debug("Loaded quest: {}", quest.getId());
                }
            } catch (Exception e) {
                FlameQuest.LOGGER.error("Failed to load quest {}: {}", entry.getKey(), e.getMessage());
            }
        }
        FlameQuest.LOGGER.info("[FlameQuest] Loaded {} quests", QUESTS.size());
    }

    public static Quest getById(String id) {
        return QUESTS.stream().filter(q -> q.getId().equals(id)).findFirst().orElse(null);
    }

    public static List<String> getCategories() {
        return QUESTS.stream().map(Quest::getCategory).distinct().sorted().toList();
    }

    public static List<Quest> getByCategory(String category) {
        return QUESTS.stream().filter(q -> category.equals(q.getCategory())).toList();
    }
}
