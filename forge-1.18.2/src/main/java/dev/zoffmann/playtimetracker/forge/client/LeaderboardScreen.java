package dev.zoffmann.playtimetracker.forge.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.zoffmann.playtimetracker.forge.client.InventoryTheme.InventoryButton;
import dev.zoffmann.playtimetracker.forge.leaderboard.CompactStatsFormat;
import dev.zoffmann.playtimetracker.forge.leaderboard.LeaderboardService.Snapshot;
import dev.zoffmann.playtimetracker.forge.leaderboard.Metric;
import dev.zoffmann.playtimetracker.forge.leaderboard.PlayerStats;
import dev.zoffmann.playtimetracker.forge.leaderboard.TrackerNetwork;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Inventory-sized leaderboard with item tabs and five rows per page. */
public final class LeaderboardScreen extends Screen {
    private static final int ROWS = 5;
    private final Screen parent;
    private final List<PlayerRow> rowButtons = new ArrayList<>();
    private final List<MetricTab> tabs = new ArrayList<>();
    private Snapshot snapshot;
    private Metric metric = Metric.PLAYTIME;
    private boolean onlyOnline;
    private String search = "";
    private UUID selectedPlayer;
    private int page, age, left, top;
    private EditBox searchBox;
    private InventoryButton previous, next, details, online;
    private List<PlayerStats> visible = List.of();

    public LeaderboardScreen(Screen parent, Snapshot snapshot) {
        super(text("title"));
        this.parent = parent;
        this.snapshot = snapshot;
    }

    public static Component text(String key, Object... args) {
        return new TranslatableComponent("playtimetracker." + key, args);
    }

    public static void open() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        minecraft.setScreen(new LeaderboardScreen(minecraft.screen, new Snapshot(List.of(), false, 0)));
        TrackerNetwork.request(false);
    }

    public static void accept(Snapshot snapshot, boolean open) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        if (minecraft.screen instanceof LeaderboardScreen screen) screen.update(snapshot);
        else if (minecraft.screen instanceof PlayerDetailsScreen details) details.update(snapshot);
        else if (open) minecraft.setScreen(new LeaderboardScreen(minecraft.screen, snapshot));
    }

    void update(Snapshot snapshot) {
        this.snapshot = snapshot;
        if (searchBox != null) rebuildRows();
    }

    private int pages() { return Math.max(1, (visible.size() + ROWS - 1) / ROWS); }

    @Override protected void init() {
        String query = searchBox == null ? search : searchBox.getValue();
        int cursor = searchBox == null ? query.length() : searchBox.getCursorPosition();
        boolean focused = searchBox != null && searchBox.isFocused();
        clearWidgets();
        rowButtons.clear();
        tabs.clear();
        left = (width - InventoryTheme.WIDTH) / 2;
        top = (height - InventoryTheme.HEIGHT) / 2;
        addRenderableWidget(new InventoryButton(left + 157, top + 6, 12, 12, new TextComponent("x"), clicked -> onClose()));
        for (Metric choice : Metric.values()) tabs.add(addRenderableWidget(new MetricTab(choice)));
        searchBox = new EditBox(font, left + 12, top + 50, 99, 10, text("search"));
        searchBox.setBordered(false);
        searchBox.setMaxLength(64);
        searchBox.setTextColor(0xFFFFFF);
        searchBox.setTextColorUneditable(0x999999);
        searchBox.setValue(query);
        searchBox.setSuggestion(query.isEmpty() ? text("search_short").getString() : null);
        searchBox.setResponder(value -> {
            search = value;
            page = 0;
            searchBox.setSuggestion(value.isEmpty() ? text("search_short").getString() : null);
            rebuildRows();
        });
        addRenderableWidget(searchBox);
        if (focused) {
            setFocused(searchBox);
            searchBox.setFocus(true);
            searchBox.setCursorPosition(cursor);
        }
        online = addRenderableWidget(new InventoryButton(left + 119, top + 48, 49, 12,
                text(onlyOnline ? "online_short" : "all_short"), clicked -> {
                    onlyOnline = !onlyOnline;
                    page = 0;
                    online.setMessage(text(onlyOnline ? "online_short" : "all_short"));
                    online.selected = onlyOnline;
                    rebuildRows();
                }));
        online.selected = onlyOnline;
        previous = addRenderableWidget(new InventoryButton(left + 8, top + 141, 19, 17,
                new TextComponent("<"), clicked -> { page--; rebuildRows(); }));
        next = addRenderableWidget(new InventoryButton(left + 30, top + 141, 19, 17,
                new TextComponent(">"), clicked -> { page++; rebuildRows(); }));
        details = addRenderableWidget(new InventoryButton(left + 105, top + 141, 63, 17,
                text("details"), clicked -> selected().ifPresent(this::showPlayer)));
        rebuildRows();
    }

    private void rebuildRows() {
        visible = metric.sorted(snapshot.players(), search, onlyOnline);
        page = Math.max(0, Math.min(page, pages() - 1));
        for (PlayerRow row : rowButtons) {
            if (getFocused() == row) setFocused(null);
            removeWidget(row);
        }
        rowButtons.clear();
        int start = page * ROWS;
        for (int row = 0; row < ROWS && start + row < visible.size(); row++) {
            rowButtons.add(addRenderableWidget(new PlayerRow(visible.get(start + row), start + row + 1, row)));
        }
        if (rowButtons.stream().noneMatch(row -> row.player.id().equals(selectedPlayer))) {
            selectedPlayer = rowButtons.isEmpty() ? null : rowButtons.get(0).player.id();
        }
        if (previous != null) previous.active = page > 0;
        if (next != null) next.active = page + 1 < pages();
        if (details != null) details.active = !rowButtons.isEmpty();
    }

    private java.util.Optional<PlayerStats> selected() {
        return visible.stream().filter(player -> player.id().equals(selectedPlayer)).findFirst();
    }

    private void showPlayer(PlayerStats player) {
        selectedPlayer = player.id();
        minecraft.setScreen(new PlayerDetailsScreen(this, player));
    }

    @Override public void tick() {
        if (searchBox != null) searchBox.tick();
        if (++age % (snapshot.loaded() ? 100 : 20) == 0) TrackerNetwork.request(false);
    }

    @Override public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
        renderBackground(pose);
        InventoryTheme.panel(pose, left, top);
        font.draw(pose, InventoryTheme.clipped(title.getString(), 144), left + 8, top + 8, InventoryTheme.TEXT);
        InventoryTheme.recess(pose, left + 8, top + 48, 107, 12, 0xFF000000);
        font.draw(pose, new TranslatableComponent(metric.translationKey()), left + 8, top + 65, InventoryTheme.TEXT);
        String sorting = text("descending").getString();
        font.draw(pose, sorting, left + 168 - font.width(sorting), top + 65, 0x555555);
        InventoryTheme.recess(pose, left + 8, top + 76, 160, 61, 0xFF8B8B8B);
        if (visible.isEmpty()) {
            String message = text(snapshot.loaded() ? "no_players" : "loading").getString();
            font.draw(pose, InventoryTheme.clipped(message, 151), left + 12, top + 81, InventoryTheme.TEXT);
        }
        font.draw(pose, (page + 1) + " / " + pages(), left + 57, top + 146, InventoryTheme.TEXT);
        super.render(pose, mouseX, mouseY, partialTick);
        for (MetricTab tab : tabs) if (tab.isHoveredOrFocused()) renderTooltip(pose, tab.getMessage(), mouseX, mouseY);
        for (PlayerRow row : rowButtons) {
            if (row.isHoveredOrFocused()) renderTooltip(pose, List.of(
                    new TextComponent(row.player.name()), InventoryTheme.status(row.player),
                    new TextComponent(new TranslatableComponent(metric.translationKey()).getString() + ": " + metric.format(row.player))),
                    java.util.Optional.empty(), mouseX, mouseY);
        }
        if (mouseX >= left + 8 && mouseX < left + 152 && mouseY >= top + 6 && mouseY < top + 18) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(text("player_count", snapshot.knownPlayers()));
            if (snapshot.knownPlayers() > snapshot.players().size()) tooltip.add(text("limited", snapshot.players().size()));
            renderTooltip(pose, tooltip, java.util.Optional.empty(), mouseX, mouseY);
        }
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        if (x >= left + 8 && x < left + 168 && y >= top + 76 && y < top + 137) {
            int target = Math.max(0, Math.min(pages() - 1, page + (amount < 0 ? 1 : -1)));
            if (target != page) { page = target; rebuildRows(); }
            return true;
        }
        return super.mouseScrolled(x, y, amount);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(parent); }

    private final class MetricTab extends InventoryButton {
        private final Metric choice;
        private final ItemStack icon;
        MetricTab(Metric choice) {
            super(left + 8 + choice.ordinal() * 27, top + 22, 25, 22,
                    new TranslatableComponent(choice.translationKey()), clicked -> {
                        metric = choice;
                        page = 0;
                        rebuildRows();
                    });
            this.choice = choice;
            icon = new ItemStack(switch (choice) {
                case PLAYTIME -> Items.CLOCK;
                case DEATHS -> Items.BONE;
                case MOB_KILLS -> Items.IRON_SWORD;
                case BLOCKS -> Items.IRON_PICKAXE;
                case DISTANCE -> Items.LEATHER_BOOTS;
                case JUMPS -> Items.SLIME_BALL;
            });
        }
        @Override public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            selected = metric == choice;
            super.renderButton(pose, mouseX, mouseY, partialTick);
        }
        @Override protected void renderContents(PoseStack pose) { itemRenderer.renderAndDecorateItem(icon, x + 5, y + 3); }
    }

    private final class PlayerRow extends Button {
        private final PlayerStats player;
        private final int rank;
        PlayerRow(PlayerStats player, int rank, int row) {
            super(left + 9, top + 77 + row * 12, 158, 11,
                    new TextComponent(player.name() + " " + metric.format(player)), clicked -> showPlayer(player));
            this.player = player;
            this.rank = rank;
        }
        @Override public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            if (isHoveredOrFocused()) fill(pose, x, y, x + width, y + height, 0xFFB0B0B0);
            String position = Integer.toString(rank);
            if (font.width(position) <= 10) font.draw(pose, position, x + 2, y + 2, 0x333333);
            InventoryTheme.head(pose, player, x + 14, y + 1, 8);
            String value = InventoryTheme.clipped(CompactStatsFormat.format(metric, player, InventoryTheme.locale()), 62);
            int valueX = left + 164 - font.width(value), statusX = Math.min(left + 106, valueX - 5);
            font.draw(pose, InventoryTheme.clipped(player.name(), Math.max(0, statusX - (x + 26) - 4)), x + 26, y + 2, 0x202020);
            font.draw(pose, value, valueX, y + 2, 0x202020);
            fill(pose, statusX, y + 3, statusX + 2, y + 7, 0xFF000000 | InventoryTheme.statusColor(player));
        }
    }
}
