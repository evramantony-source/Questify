package com.flamefragger45.flamequest.data;

import com.google.gson.annotations.SerializedName;

public class QuestReward {
    public enum Type { ITEM, XP }

    @SerializedName("type")   private String type;
    @SerializedName("item")   private String item;
    @SerializedName("count")  private int count = 1;
    @SerializedName("amount") private int amount = 0;

    public Type getType()   { return "xp".equalsIgnoreCase(type) ? Type.XP : Type.ITEM; }
    public String getItem() { return item; }
    public int getCount()   { return count; }
    public int getAmount()  { return amount; }
}
