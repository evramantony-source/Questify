package com.flamefragger45.flamequest.client.screen;

import com.flamefragger45.flamequest.client.ClientQuestData;
import com.flamefragger45.flamequest.data.*;
import com.flamefragger45.flamequest.network.NetworkHandler;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.List;

@Environment(EnvType.CLIENT)
public class QuestBookScreen extends Screen {

    // Layout constants
    private static final int GUI_W = 340;
    private static final int GUI_H = 230;
    private static final int CAT_W = 82;
    private static final int QUEST_W = 120;
    private static final int TITLE_H = 18;
    private static final int ROW_H = 22;

    // Colors (ARGB)
    private static final int C_BG       = 0xFF1E1E2E;
    private static final int C_PANEL    = 0xFF2A2A3A;
    private static final int C_SELECTED = 0xFF3D5A80;
    private static final int C_COMPLETE = 0xFF2D6A4F;
    private static final int C_LOCKED   = 0xFF3A3A3A;
    private static final int C_BORDER   = 0xFF4A4A6A;
    private static final int C_GOLD     = 0xFFFFD700;
    private static final int C_WHITE    = 0xFFFFFFFF;
    private static final int C_GRAY     = 0xFFAAAAAA;
    private static final int C_GREEN    = 0xFF52B788;
    private static final int C_AMBER    = 0xFFE9B44C;
    private static final int C_RED      = 0xFFE63946;

    // State
    private int guiLeft, guiTop;
    private String selectedCategory = null;
    private Quest selectedQuest = null;

    // Scroll offsets
    private int catScroll   = 0;
    private int questScroll = 0;

    public QuestBookScreen(Text title) {
        super(title);
    }

    @Override
    public void init() {
        super.init();
        guiLeft = (this.width  - GUI_W) / 2;
        guiTop  = (this.height - GUI_H) / 2;

        List<String> categories = ClientQuestData.getCategories();
        if (selectedCategory == null && !categories.isEmpty()) {
            selectedCategory = categories.get(0);
        }

        rebuildButtons();
    }

    // ── Button construction ───────────────────────────────────────────────────

    private void rebuildButtons() {
        this.clearChildren();

        // Close button
        addDrawableChild(ButtonWidget.builder(Text.literal("✕"), btn -> this.close())
            .dimensions(guiLeft + GUI_W - 18, guiTop + 2, 16, 14)
            .build());

        // Category buttons
        List<String> categories = ClientQuestData.getCategories();
        int maxCatRows = (GUI_H - TITLE_H - 4) / ROW_H;
        int startCat = Math.min(catScroll, Math.max(0, categories.size() - maxCatRows));
        catScroll = startCat;

        for (int i = 0; i < maxCatRows && (i + startCat) < categories.size(); i++) {
            String cat = categories.get(i + startCat);
            boolean sel = cat.equals(selectedCategory);
            int y = guiTop + TITLE_H + 2 + i * ROW_H;
            String label = cat.length() > 10 ? cat.substring(0, 9) + "…" : cat;
            addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                selectedCategory = cat;
                selectedQuest    = null;
                questScroll      = 0;
                rebuildButtons();
            }).dimensions(guiLeft + 2, y, CAT_W - 4, ROW_H - 2).build());
        }

        // Cat scroll arrows
        if (catScroll > 0) {
            addDrawableChild(ButtonWidget.builder(Text.literal("▲"), btn -> {
                catScroll = Math.max(0, catScroll - 1);
                rebuildButtons();
            }).dimensions(guiLeft + CAT_W / 2 - 8, guiTop + TITLE_H, 16, 12).build());
        }
        if (startCat + maxCatRows < categories.size()) {
            addDrawableChild(ButtonWidget.builder(Text.literal("▼"), btn -> {
                catScroll++;
                rebuildButtons();
            }).dimensions(guiLeft + CAT_W / 2 - 8, guiTop + GUI_H - 14, 16, 12).build());
        }

        // Quest list buttons
        if (selectedCategory != null) {
            List<Quest> quests = ClientQuestData.getByCategory(selectedCategory);
            int maxQRows = (GUI_H - TITLE_H - 4) / ROW_H;
            int startQ = Math.min(questScroll, Math.max(0, quests.size() - maxQRows));
            questScroll = startQ;

            for (int i = 0; i < maxQRows && (i + startQ) < quests.size(); i++) {
                Quest quest = quests.get(i + startQ);
                int y = guiTop + TITLE_H + 2 + i * ROW_H;
                boolean available = ClientQuestData.isAvailable(quest);
                QuestProgress prog = ClientQuestData.getProgress(quest.getId());
                String label = (quest.getTitle().length() > 13 ? quest.getTitle().substring(0, 12) + "…" : quest.getTitle());

                addDrawableChild(ButtonWidget.builder(Text.literal(label), btn -> {
                    if (available) {
                        selectedQuest = quest;
                        rebuildButtons();
                    }
                }).dimensions(guiLeft + CAT_W + 2, y, QUEST_W - 4, ROW_H - 2).build());
            }

            // Quest scroll arrows
            if (questScroll > 0) {
                addDrawableChild(ButtonWidget.builder(Text.literal("▲"), btn -> {
                    questScroll = Math.max(0, questScroll - 1);
                    rebuildButtons();
                }).dimensions(guiLeft + CAT_W + QUEST_W / 2 - 8, guiTop + TITLE_H, 16, 12).build());
            }
            if (startQ + maxQRows < quests.size()) {
                addDrawableChild(ButtonWidget.builder(Text.literal("▼"), btn -> {
                    questScroll++;
                    rebuildButtons();
                }).dimensions(guiLeft + CAT_W + QUEST_W / 2 - 8, guiTop + GUI_H - 14, 16, 12).build());
            }
        }

        // Claim reward button
        if (selectedQuest != null) {
            QuestProgress prog = ClientQuestData.getProgress(selectedQuest.getId());
            boolean canClaim  = prog.isCompleted() && !prog.isRewarded();
            boolean available = ClientQuestData.isAvailable(selectedQuest);
            int detailX = guiLeft + CAT_W + QUEST_W + 2;
            int detailW = GUI_W - CAT_W - QUEST_W - 4;
            int btnY    = guiTop + GUI_H - 26;

            if (available) {
                String btnLabel = prog.isRewarded() ? "✓ Claimed"
                    : (prog.isCompleted() ? "⭐ Claim Reward!" : "In Progress…");
                addDrawableChild(ButtonWidget.builder(Text.literal(btnLabel), btn -> {
                    if (canClaim) {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeString(selectedQuest.getId(), 256);
                        ClientPlayNetworking.send(NetworkHandler.CLAIM_REWARD_ID, buf);
                    }
                }).dimensions(detailX, btnY, detailW, 20).build());
            }
        }
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        renderBackground(ctx);
        drawPanels(ctx);
        drawDetailPanel(ctx);
        super.render(ctx, mx, my, delta);  // draws widgets on top
    }

    private void drawPanels(DrawContext ctx) {
        // Main background
        ctx.fill(guiLeft, guiTop, guiLeft + GUI_W, guiTop + GUI_H, C_BG);

        // Title bar
        ctx.fill(guiLeft, guiTop, guiLeft + GUI_W, guiTop + TITLE_H, C_PANEL);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("✦ FlameQuest ✦").styled(s -> s.withColor(C_GOLD)),
            guiLeft + GUI_W / 2, guiTop + 4, C_WHITE);

        // Category panel bg
        ctx.fill(guiLeft, guiTop + TITLE_H, guiLeft + CAT_W, guiTop + GUI_H, C_PANEL);
        // Category label
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.literal("Categories").styled(s -> s.withColor(C_GRAY)),
            guiLeft + CAT_W / 2, guiTop + TITLE_H + 4, C_WHITE);

        // Quest panel bg
        ctx.fill(guiLeft + CAT_W, guiTop + TITLE_H, guiLeft + CAT_W + QUEST_W, guiTop + GUI_H, C_BG);
        if (selectedCategory != null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(selectedCategory).styled(s -> s.withColor(C_AMBER)),
                guiLeft + CAT_W + QUEST_W / 2, guiTop + TITLE_H + 4, C_WHITE);
        }

        // Detail panel bg
        int detailX = guiLeft + CAT_W + QUEST_W;
        ctx.fill(detailX, guiTop + TITLE_H, guiLeft + GUI_W, guiTop + GUI_H, C_PANEL);

        // Borders
        drawBorder(ctx, guiLeft, guiTop, GUI_W, GUI_H, C_BORDER);
        vLine(ctx, guiLeft + CAT_W, guiTop + TITLE_H, guiTop + GUI_H, C_BORDER);
        vLine(ctx, detailX, guiTop + TITLE_H, guiTop + GUI_H, C_BORDER);
        hLine(ctx, guiLeft, guiLeft + GUI_W, guiTop + TITLE_H, C_BORDER);

        // Quest status dots next to each quest button
        if (selectedCategory != null) {
            List<Quest> quests = ClientQuestData.getByCategory(selectedCategory);
            int maxQRows = (GUI_H - TITLE_H - 4) / ROW_H;
            for (int i = 0; i < maxQRows && (i + questScroll) < quests.size(); i++) {
                Quest quest = quests.get(i + questScroll);
                QuestProgress prog = ClientQuestData.getProgress(quest.getId());
                boolean available = ClientQuestData.isAvailable(quest);
                int dotColor = !available ? C_LOCKED
                    : prog.isRewarded()  ? C_COMPLETE
                    : prog.isCompleted() ? C_GREEN
                    : C_AMBER;
                int y = guiTop + TITLE_H + 2 + i * ROW_H + (ROW_H - 2) / 2 - 2;
                ctx.fill(guiLeft + CAT_W + QUEST_W - 8, y, guiLeft + CAT_W + QUEST_W - 4, y + 4, dotColor);
            }
        }
    }

    private void drawDetailPanel(DrawContext ctx) {
        int detailX = guiLeft + CAT_W + QUEST_W + 4;
        int detailW = GUI_W - CAT_W - QUEST_W - 8;
        int y       = guiTop + TITLE_H + 4;
        int maxY    = guiTop + GUI_H - 28;

        if (selectedQuest == null) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("← Select a quest").styled(s -> s.withColor(C_GRAY)),
                detailX + detailW / 2, guiTop + GUI_H / 2, C_WHITE);
            return;
        }

        Quest q = selectedQuest;
        QuestProgress prog = ClientQuestData.getProgress(q.getId());
        boolean available = ClientQuestData.isAvailable(q);

        // Quest icon
        Identifier iconId = new Identifier(q.getIcon());
        ItemStack iconStack = Registries.ITEM.containsId(iconId)
            ? new ItemStack(Registries.ITEM.get(iconId))
            : new ItemStack(Items.BOOK);
        ctx.drawItem(iconStack, detailX, y);


        // Status badge
        String status = !available ? "🔒 Locked"
            : prog.isRewarded()   ? "✓ Done"
            : prog.isCompleted()  ? "⭐ Complete!"
            : "○ In Progress";
        int statusColor = !available ? C_GRAY
            : prog.isRewarded()   ? C_COMPLETE
            : prog.isCompleted()  ? C_GOLD
            : C_AMBER;
        ctx.drawTextWithShadow(textRenderer, Text.literal(status).styled(s -> s.withColor(statusColor)),
            detailX + 20, y + 4, C_WHITE);
        y += 20;

        // Title
        ctx.drawTextWithShadow(textRenderer,
            Text.literal(q.getTitle()).styled(s -> s.withColor(C_GOLD).withBold(true)),
            detailX, y, C_WHITE);
        y += 11;

        // Description (word-wrap at detailW chars)
        for (String line : wrapText(q.getDescription(), detailW)) {
            if (y + 9 > maxY) break;
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(line).styled(s -> s.withColor(C_GRAY)),
                detailX, y, C_WHITE);
            y += 9;
        }
        y += 4;

        // Objectives header
        if (y + 9 < maxY) {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("Objectives:").styled(s -> s.withColor(C_AMBER)),
                detailX, y, C_WHITE);
            y += 10;
        }

        for (int i = 0; i < q.getObjectives().size(); i++) {
            if (y + 9 > maxY) break;
            QuestObjective obj = q.getObjectives().get(i);
            int cur = prog.getObjectiveProgress(i);
            int max = obj.getCount();
            boolean done = cur >= max;
            String tick = done ? "✓ " : "○ ";
            int txtColor = done ? C_GREEN : C_WHITE;
            String desc = obj.getDescription() != null ? obj.getDescription()
                : obj.getTarget() + " x" + max;
            String progStr = max > 1 ? " (" + Math.min(cur, max) + "/" + max + ")" : "";
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(tick + desc + progStr).styled(s -> s.withColor(txtColor)),
                detailX, y, C_WHITE);
            y += 9;
        }
        y += 4;

        // Rewards header
        if (y + 9 < maxY) {
            ctx.drawTextWithShadow(textRenderer,
                Text.literal("Rewards:").styled(s -> s.withColor(C_AMBER)),
                detailX, y, C_WHITE);
            y += 10;
        }

        for (QuestReward reward : q.getRewards()) {
            if (y + 9 > maxY) break;
            String rewardText;
            if (reward.getType() == QuestReward.Type.XP) {
                rewardText = "⭐ " + reward.getAmount() + " XP";
            } else {
                String itemName = reward.getItem() != null
                    ? reward.getItem().replace("minecraft:", "") : "?";
                rewardText = "▪ " + itemName + " x" + reward.getCount();
            }
            ctx.drawTextWithShadow(textRenderer,
                Text.literal(rewardText).styled(s -> s.withColor(C_WHITE)),
                detailX, y, C_WHITE);
            y += 9;
        }

        if (!available) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Finish prerequisites first!").styled(s -> s.withColor(C_RED)),
                detailX + detailW / 2, maxY - 14, C_WHITE);
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void drawBorder(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x,         y,         x + w, y + 1,     color);
        ctx.fill(x,         y + h - 1, x + w, y + h,     color);
        ctx.fill(x,         y,         x + 1, y + h,     color);
        ctx.fill(x + w - 1, y,         x + w, y + h,     color);
    }

    private void vLine(DrawContext ctx, int x, int y1, int y2, int color) {
        ctx.fill(x, y1, x + 1, y2, color);
    }

    private void hLine(DrawContext ctx, int x1, int x2, int y, int color) {
        ctx.fill(x1, y, x2, y + 1, color);
    }

    private java.util.List<String> wrapText(String text, int maxPx) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (text == null) return lines;
        // Rough char wrap: ~6px per char average
        int charsPerLine = Math.max(10, maxPx / 6);
        String[] words = text.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (sb.length() + word.length() + 1 > charsPerLine) {
                lines.add(sb.toString().trim());
                sb = new StringBuilder();
            }
            sb.append(word).append(" ");
        }
        if (!sb.isEmpty()) lines.add(sb.toString().trim());
        return lines;
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean isPauseScreen() { return false; }
}
