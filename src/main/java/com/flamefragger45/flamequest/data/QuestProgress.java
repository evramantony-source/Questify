package com.flamefragger45.flamequest.data;

import net.minecraft.nbt.NbtCompound;
import java.util.HashMap;
import java.util.Map;

public class QuestProgress {
    private final String questId;
    private final Map<Integer, Integer> objectiveProgress = new HashMap<>();
    private boolean completed = false;
    private boolean rewarded  = false;

    public QuestProgress(String questId) { this.questId = questId; }

    public String getQuestId() { return questId; }

    public int getObjectiveProgress(int index) {
        return objectiveProgress.getOrDefault(index, 0);
    }

    public void addObjectiveProgress(int index, int amount) {
        objectiveProgress.merge(index, amount, Integer::sum);
    }

    public void setObjectiveProgress(int index, int value) {
        objectiveProgress.put(index, value);
    }

    public boolean isCompleted()             { return completed; }
    public void setCompleted(boolean v)      { this.completed = v; }
    public boolean isRewarded()              { return rewarded; }
    public void setRewarded(boolean v)       { this.rewarded = v; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("questId", questId);
        nbt.putBoolean("completed", completed);
        nbt.putBoolean("rewarded", rewarded);
        NbtCompound prog = new NbtCompound();
        objectiveProgress.forEach((k, v) -> prog.putInt(String.valueOf(k), v));
        nbt.put("progress", prog);
        return nbt;
    }

    public static QuestProgress fromNbt(NbtCompound nbt) {
        QuestProgress p = new QuestProgress(nbt.getString("questId"));
        p.completed = nbt.getBoolean("completed");
        p.rewarded  = nbt.getBoolean("rewarded");
        NbtCompound prog = nbt.getCompound("progress");
        for (String key : prog.getKeys()) {
            p.objectiveProgress.put(Integer.parseInt(key), prog.getInt(key));
        }
        return p;
    }
}
