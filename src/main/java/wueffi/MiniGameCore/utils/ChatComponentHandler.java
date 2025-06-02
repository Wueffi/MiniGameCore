package wueffi.MiniGameCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ChatComponentHandler {
    public static TextComponent CreateBaseComponent() {
        return Component.text("[").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("MiniGameCore").color(NamedTextColor.GOLD))
                .append(Component.text("]").color(NamedTextColor.DARK_GRAY));
    }

    public static TextComponent AddColorText(TextComponent component, String text, NamedTextColor color) {
        return component.append(Component.text(text).color(color));
    }

    public static TextComponent AddColorText(TextComponent component, String text, TextColor color) {
        return component.append(Component.text(text).color(color));
    }
}
