package dev.zoffmann.playtimetracker.forge;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TabDisplayManagerTest {
    @Test
    void displaysWholeHoursAndAfkAfterTheName() {
        Component base = new TextComponent("Alex");
        assertEquals("Alex [12h]", TabDisplayManager.decorate(base, 12L * 72000, false).getString());
        assertEquals("Alex [12h] [AFK]", TabDisplayManager.decorate(base, 12L * 72000, true).getString());
    }

    @Test
    void preservesExistingModFormattingWithoutMutatingTheNameOrColoringSuffixes() {
        Component base = new TextComponent("[Guild] ").withStyle(ChatFormatting.BLUE)
                .append(new TextComponent("Alex").withStyle(ChatFormatting.BOLD));
        String original = Component.Serializer.toJson(base);
        Component result = TabDisplayManager.decorate(base, 72000, true);
        assertEquals("[Guild] Alex [1h] [AFK]", result.getString());
        assertEquals(original, Component.Serializer.toJson(base));
        assertEquals(original, Component.Serializer.toJson(result.getSiblings().get(0)));
        assertNull(result.getSiblings().get(1).getStyle().getColor());
    }
}
