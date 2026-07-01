package com.flamefragger45.flamequest.item;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item QUEST_BOOK = Registry.register(
        Registries.ITEM,
        new Identifier("flamequest", "quest_book"),
        new QuestBookItem(new Item.Settings().maxCount(1))
    );

    public static void register() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(QUEST_BOOK);
        });
    }
}
