package dev.zoffmann.playtimetracker.forge.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.zoffmann.playtimetracker.forge.leaderboard.PlayerStats;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Vanilla inventory dimensions and pixel bevels shared by both leaderboard views. */
final class InventoryTheme extends GuiComponent {
    static final int WIDTH = 176, HEIGHT = 166, TEXT = 0x404040;
    private static final ResourceLocation INVENTORY = new ResourceLocation("minecraft:textures/gui/container/inventory.png");

    static Locale locale() {
        return Locale.forLanguageTag(Minecraft.getInstance().getLanguageManager().getSelected().getCode().replace('_', '-'));
    }

    static void panel(PoseStack pose, int x, int y) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, INVENTORY);
        GuiComponent.blit(pose, x, y, 0, 0, WIDTH, HEIGHT, 256, 256);
        fill(pose, x + 4, y + 4, x + 172, y + 162, 0xFFC6C6C6);
    }

    static void recess(PoseStack pose, int x, int y, int width, int height, int color) {
        fill(pose, x, y, x + width, y + height, 0xFF373737);
        fill(pose, x + 1, y + 1, x + width, y + height, 0xFFFFFFFF);
        fill(pose, x + 1, y + 1, x + width - 1, y + height - 1, color);
    }

    static void head(PoseStack pose, PlayerStats player, int x, int y, int size) {
        Minecraft minecraft = Minecraft.getInstance();
        var info = minecraft.getConnection() == null ? null : minecraft.getConnection().getPlayerInfo(player.id());
        ResourceLocation skin = info == null ? DefaultPlayerSkin.getDefaultSkin(player.id()) : info.getSkinLocation();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.setShaderTexture(0, skin);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        blit(pose, x, y, size, size, 8, 8, 8, 8, 64, 64);
        blit(pose, x, y, size, size, 40, 8, 8, 8, 64, 64);
        RenderSystem.disableBlend();
    }

    static String clipped(String value, int width) {
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= width) return value;
        return font.plainSubstrByWidth(value, Math.max(0, width - font.width(".."))) + "..";
    }

    static Component status(PlayerStats player) {
        return LeaderboardScreen.text(player.afk() ? "afk" : player.online() ? "online" : "offline");
    }

    static int statusColor(PlayerStats player) { return player.afk() ? 0x77500D : player.online() ? 0x346B20 : 0x555555; }

    static class InventoryButton extends Button {
        boolean selected;
        InventoryButton(int x, int y, int width, int height, Component label, OnPress action) {
            super(x, y, width, height, label, action);
        }

        @Override public void renderButton(PoseStack pose, int mouseX, int mouseY, float partialTick) {
            fill(pose, x, y, x + width, y + height, 0xFF000000);
            fill(pose, x + 1, y + 1, x + width - 1, y + height - 1,
                    isHoveredOrFocused() && active && !selected ? 0xFF8E97B4 : 0xFF8B8B8B);
            int light = selected ? 0xFF545454 : 0xFFC6C6C6, dark = selected ? 0xFFC6C6C6 : 0xFF545454;
            fill(pose, x + 1, y + 1, x + width - 1, y + 2, light);
            fill(pose, x + 1, y + 1, x + 2, y + height - 1, light);
            fill(pose, x + 1, y + height - 2, x + width - 1, y + height - 1, dark);
            fill(pose, x + width - 2, y + 1, x + width - 1, y + height - 1, dark);
            renderContents(pose);
        }

        protected void renderContents(PoseStack pose) {
            var font = Minecraft.getInstance().font;
            String label = clipped(getMessage().getString(), width - 4);
            int textX = x + (width - font.width(label)) / 2, textY = y + (height - 8) / 2;
            if (active) font.draw(pose, label, textX + 1, textY + 1, 0x3F3F3F);
            font.draw(pose, label, textX, textY, !active ? 0x575757 : selected ? 0xFFFFA0 : 0xFFFFFF);
        }
    }
    private InventoryTheme() {}
}
