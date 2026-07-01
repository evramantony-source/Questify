package com.flamefragger45.flamequest.data;

import com.flamefragger45.flamequest.FlameQuest;
import com.flamefragger45.flamequest.network.NetworkHandler;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.PersistentState;

import java.util.*;

public class QuestManager extends PersistentState {

    private static final String DATA_KEY = "flamequest_data";
    // uuid -> (questId -> progress)
    private final Map<UUID, Map<String, QuestProgress>> allPlayerData = new HashMap<>();

    // ── Singleton access ─────────────────────────────────────────────────────
    public static QuestManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
            .getOrCreate(
                new PersistentState.Type<>(QuestManager::new, QuestManager::fromNbt, null),
                DATA_KEY
            );
    }

    // ── Per-player helpers ────────────────────────────────────────────────────
    public Map<String, QuestProgress> getPlayerData(UUID uuid) {
        return allPlayerData.computeIfAbsent(uuid, k -> new HashMap<>());
    }

    public QuestProgress getProgress(UUID uuid, String questId) {
        return getPlayerData(uuid).computeIfAbsent(questId, QuestProgress::new);
    }

    // ── Event entry points ────────────────────────────────────────────────────

    /** Called on kill events */
    public void onKill(ServerPlayerEntity player, String entityId) {
        UUID uuid = player.getUuid();
        boolean changed = false;
        for (Quest quest : QuestLoader.QUESTS) {
            if (!isAvailable(uuid, quest)) continue;
            QuestProgress prog = getProgress(uuid, quest.getId());
            if (prog.isCompleted()) continue;
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (obj.getType() == QuestObjective.Type.KILL
                        && entityId.equals(obj.getTarget())
                        && prog.getObjectiveProgress(i) < obj.getCount()) {
                    prog.addObjectiveProgress(i, 1);
                    changed = true;
                }
            }
        }
        if (changed) {
            checkCompletions(player);
            markDirty();
            NetworkHandler.syncToClient(player);
        }
    }

    /** Called on craft events */
    public void onCraft(ServerPlayerEntity player, String itemId) {
        UUID uuid = player.getUuid();
        boolean changed = false;
        for (Quest quest : QuestLoader.QUESTS) {
            if (!isAvailable(uuid, quest)) continue;
            QuestProgress prog = getProgress(uuid, quest.getId());
            if (prog.isCompleted()) continue;
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (obj.getType() == QuestObjective.Type.CRAFT
                        && itemId.equals(obj.getTarget())
                        && prog.getObjectiveProgress(i) < obj.getCount()) {
                    prog.addObjectiveProgress(i, 1);
                    changed = true;
                }
            }
        }
        if (changed) {
            checkCompletions(player);
            markDirty();
            NetworkHandler.syncToClient(player);
        }
    }

    /** Called every second (tick) to scan inventories for COLLECT quests */
    public void tickPlayer(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        boolean changed = false;
        for (Quest quest : QuestLoader.QUESTS) {
            if (!isAvailable(uuid, quest)) continue;
            QuestProgress prog = getProgress(uuid, quest.getId());
            if (prog.isCompleted()) continue;
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (obj.getType() != QuestObjective.Type.COLLECT) continue;
                int current = countInInventory(player, obj.getTarget());
                int capped = Math.min(current, obj.getCount());
                if (prog.getObjectiveProgress(i) != capped) {
                    prog.setObjectiveProgress(i, capped);
                    changed = true;
                }
            }
        }
        if (changed) {
            checkCompletions(player);
            markDirty();
            NetworkHandler.syncToClient(player);
        }
    }

    /** Server-side reward claim, triggered from client packet */
    public void claimReward(ServerPlayerEntity player, String questId) {
        UUID uuid = player.getUuid();
        QuestProgress prog = getProgress(uuid, questId);
        if (!prog.isCompleted() || prog.isRewarded()) return;

        Quest quest = QuestLoader.getById(questId);
        if (quest == null) return;

        for (QuestReward reward : quest.getRewards()) {
            if (reward.getType() == QuestReward.Type.ITEM && reward.getItem() != null) {
                Identifier id = new Identifier(reward.getItem());
                if (Registries.ITEM.containsId(id)) {
                    ItemStack stack = new ItemStack(Registries.ITEM.get(id), reward.getCount());
                    if (!player.getInventory().insertStack(stack)) {
                        player.dropItem(stack, false);
                    }
                }
            } else if (reward.getType() == QuestReward.Type.XP) {
                player.addExperience(reward.getAmount());
            }
        }

        prog.setRewarded(true);
        markDirty();
        player.sendMessage(
            Text.literal("[FlameQuest] ").formatted(Formatting.GOLD)
                .append(Text.literal("Rewards claimed for: " + quest.getTitle()).formatted(Formatting.GREEN)),
            false
        );
        NetworkHandler.syncToClient(player);
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private boolean isAvailable(UUID uuid, Quest quest) {
        for (String pre : quest.getPrerequisites()) {
            QuestProgress p = getPlayerData(uuid).get(pre);
            if (p == null || !p.isCompleted()) return false;
        }
        return true;
    }

    private void checkCompletions(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();
        for (Quest quest : QuestLoader.QUESTS) {
            QuestProgress prog = getProgress(uuid, quest.getId());
            if (prog.isCompleted()) continue;
            boolean allDone = true;
            for (int i = 0; i < quest.getObjectives().size(); i++) {
                QuestObjective obj = quest.getObjectives().get(i);
                if (prog.getObjectiveProgress(i) < obj.getCount()) {
                    allDone = false;
                    break;
                }
            }
            if (allDone) {
                prog.setCompleted(true);
                player.sendMessage(
                    Text.literal("[FlameQuest] ").formatted(Formatting.GOLD)
                        .append(Text.literal("Quest Complete: " + quest.getTitle()).formatted(Formatting.YELLOW)),
                    false
                );
                FlameQuest.LOGGER.info("Player {} completed quest: {}", player.getName().getString(), quest.getId());
            }
        }
    }

    private int countInInventory(ServerPlayerEntity player, String itemId) {
        Identifier id = new Identifier(itemId);
        if (!Registries.ITEM.containsId(id)) return 0;
        var item = Registries.ITEM.get(id);
        PlayerInventory inv = player.getInventory();
        int count = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                count += stack.getCount();
            }
        }
        return count;
    }

    // ── NBT serialisation ─────────────────────────────────────────────────────

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList playerList = new NbtList();
        for (Map.Entry<UUID, Map<String, QuestProgress>> entry : allPlayerData.entrySet()) {
            NbtCompound playerNbt = new NbtCompound();
            playerNbt.putUuid("uuid", entry.getKey());
            NbtList questList = new NbtList();
            for (QuestProgress prog : entry.getValue().values()) {
                questList.add(prog.toNbt());
            }
            playerNbt.put("quests", questList);
            playerList.add(playerNbt);
        }
        nbt.put("players", playerList);
        return nbt;
    }

    public static QuestManager fromNbt(NbtCompound nbt) {
        QuestManager manager = new QuestManager();
        NbtList playerList = nbt.getList("players", 10);
        for (int i = 0; i < playerList.size(); i++) {
            NbtCompound playerNbt = playerList.getCompound(i);
            UUID uuid = playerNbt.getUuid("uuid");
            Map<String, QuestProgress> questMap = new HashMap<>();
            NbtList questList = playerNbt.getList("quests", 10);
            for (int j = 0; j < questList.size(); j++) {
                QuestProgress p = QuestProgress.fromNbt(questList.getCompound(j));
                questMap.put(p.getQuestId(), p);
            }
            manager.allPlayerData.put(uuid, questMap);
        }
        return manager;
    }
}
