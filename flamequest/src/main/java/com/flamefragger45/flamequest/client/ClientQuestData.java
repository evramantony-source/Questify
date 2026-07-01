package com.flamefragger45.flamequest.client;

import com.flamefragger45.flamequest.data.Quest;
import com.flamefragger45.flamequest.data.QuestProgress;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.*;

@Environment(EnvType.CLIENT)
public class ClientQuestData {

    private static List<Quest> quests   = new ArrayList<>();
    private static Map<String, QuestProgress> progress = new HashMap<>();

    public static void update(List<Quest> q, Map<String, QuestProgress> p) {
        quests   = q;
        progress = p;
    }

    public static List<Quest> getQuests() { return quests; }

    public static QuestProgress getProgress(String questId) {
        return progress.getOrDefault(questId, new QuestProgress(questId));
    }

    public static List<String> getCategories() {
        return quests.stream().map(Quest::getCategory).distinct().sorted().toList();
    }

    public static List<Quest> getByCategory(String category) {
        return quests.stream().filter(q -> category.equals(q.getCategory())).toList();
    }

    public static boolean isAvailable(Quest quest) {
        for (String pre : quest.getPrerequisites()) {
            QuestProgress p = progress.get(pre);
            if (p == null || !p.isCompleted()) return false;
        }
        return true;
    }
}
