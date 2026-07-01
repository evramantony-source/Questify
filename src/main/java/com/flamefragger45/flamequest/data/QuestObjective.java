package com.flamefragger45.flamequest.data;

import com.google.gson.annotations.SerializedName;

public class QuestObjective {
    public enum Type { KILL, COLLECT, CRAFT }

    @SerializedName("type")        private String type;
    @SerializedName("target")      private String target;
    @SerializedName("count")       private int count = 1;
    @SerializedName("description") private String description;

    public Type getType() {
        if (type == null) return Type.COLLECT;
        return switch (type.toLowerCase()) {
            case "kill"  -> Type.KILL;
            case "craft" -> Type.CRAFT;
            default      -> Type.COLLECT;
        };
    }
    public String getTarget()      { return target; }
    public int getCount()          { return count; }
    public String getDescription() { return description; }
}
