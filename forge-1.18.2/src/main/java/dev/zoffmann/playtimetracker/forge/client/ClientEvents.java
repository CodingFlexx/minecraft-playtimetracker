package dev.zoffmann.playtimetracker.forge.client;

import dev.zoffmann.playtimetracker.forge.PlaytimeTrackerMod;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = PlaytimeTrackerMod.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    @SubscribeEvent
    public static void inventory(ScreenEvent.InitScreenEvent.Post event) {
        // FTB Library reads our sidebar_buttons.json when present, positioning
        // the leaderboard alongside Teams/Quests without a hard dependency.
        if (ModList.get().isLoaded("ftblibrary")) return;
        if (!(event.getScreen() instanceof InventoryScreen || event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) event.getScreen();
        int x = Math.max(2, screen.getGuiLeft() - 24);
        int y = screen.getGuiTop() + 4;
        event.addListener(new Button(x, y, 20, 20, new TextComponent("L"), clicked -> LeaderboardScreen.open(),
                (button, pose, mouseX, mouseY) -> screen.renderTooltip(pose, LeaderboardScreen.text("title"), mouseX, mouseY)));
    }
    private ClientEvents() {}
}
