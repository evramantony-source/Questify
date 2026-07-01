package com.flamefragger45.flamequest.data;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;

public class Quest {
    @SerializedName("id")            private String id;
    @SerializedName("title")         private String title;
    @SerializedName("description")   private String description;
    @SerializedName("category")      private String category;
    @SerializedName("icon")          private String icon = "minecraft:book";
    @SerializedName("prerequisites") private List<String> prerequisites = new ArrayList<>();
    @SerializedName("objectives")    private List<QuestObjective> objectives = new ArrayList<>();
    @SerializedName("rewards")       private List<QuestReward> rewards = new ArrayList<>();

    public String getId()                       { return id; }
    public String getTitle()                    { return title; }
    public String getDescription()              { return description; }
    public String getCategory()                 { return category; }
    public String getIcon()                     { return icon; }
    public List<String> getPrerequisites()      { return prerequisites; }
    public List<QuestObjective> getObjectives() { return objectives; }
    public List<QuestReward> getRewards()       { return rewards; }
}
