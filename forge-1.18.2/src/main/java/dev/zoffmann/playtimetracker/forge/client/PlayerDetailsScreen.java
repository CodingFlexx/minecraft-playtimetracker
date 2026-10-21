package dev.zoffmann.playtimetracker.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zoffmann.playtimetracker.forge.client.InventoryTheme.InventoryButton;
import dev.zoffmann.playtimetracker.forge.leaderboard.CompactStatsFormat;
import dev.zoffmann.playtimetracker.forge.leaderboard.LeaderboardService.Snapshot;
import dev.zoffmann.playtimetracker.forge.leaderboard.Metric;
import dev.zoffmann.playtimetracker.forge.leaderboard.PlayerStats;
import dev.zoffmann.playtimetracker.forge.leaderboard.TrackerNetwork;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

final class PlayerDetailsScreen extends Screen {
    private final LeaderboardScreen parent;
    private PlayerStats player;
    private int left, top, age;

    PlayerDetailsScreen(LeaderboardScreen parent, PlayerStats player) {
        super(LeaderboardScreen.text("player_details"));
        this.parent = parent;
        this.player = player;
    }
    void update(Snapshot snapshot) {
        parent.update(snapshot);
        player = snapshot.players().stream().filter(p -> p.id().equals(player.id())).findFirst().orElse(player);
    }
    @Override protected void init() {
        clearWidgets();
        left = (width - InventoryTheme.WIDTH) / 2;
        top = (height - InventoryTheme.HEIGHT) / 2;
        addRenderableWidget(new InventoryButton(left + 157, top + 6, 12, 12, new TextComponent("x"), clicked -> onClose()));
        addRenderableWidget(new InventoryButton(left + 8, top + 143, 160, 16,
                LeaderboardScreen.text("back"), clicked -> onClose()));
    }
    @Override public void tick() { if (++age % 100 == 0) TrackerNetwork.request(false); }
    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        InventoryTheme.panel(pose, left, top);
        font.draw(pose, title, left + 8, top + 8, InventoryTheme.TEXT);
        InventoryTheme.recess(pose, left + 8, top + 24, 28, 28, 0xFF6B6B6B);
        InventoryTheme.head(pose, player, left + 10, top + 26, 24);
        font.draw(pose, InventoryTheme.clipped(player.name(), 126), left + 42, top + 27, InventoryTheme.TEXT);
        font.draw(pose, InventoryTheme.status(player), left + 42, top + 40, InventoryTheme.statusColor(player));
        for (Metric metric : Metric.values()) {
            int x = left + 8 + (metric.ordinal() % 2) * 82, y = top + 58 + (metric.ordinal() / 2) * 27;
            InventoryTheme.recess(pose, x, y, 78, 24, 0xFFA6A6A6);
            font.draw(pose, new TranslatableComponent(metric.translationKey()), x + 4, y + 3, 0x333333);
            String value = CompactStatsFormat.format(metric, player, InventoryTheme.locale());
            font.draw(pose, InventoryTheme.clipped(value, 70), x + 4, y + 13, 0x202020);
        }
        super.render(pose, mouseX, mouseY, partialTick);
        for (Metric metric : Metric.values()) {
            int x = left + 8 + (metric.ordinal() % 2) * 82, y = top + 58 + (metric.ordinal() / 2) * 27;
            if (mouseX >= x && mouseX < x + 78 && mouseY >= y && mouseY < y + 24) {
                renderTooltip(pose, new TextComponent(metric.format(player)), mouseX, mouseY);
            }
        }
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
