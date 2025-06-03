package wueffi.MiniGameCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ComponentFactory {
    TextComponent component;

    public ComponentFactory() {
        component = Component.text("[").color(NamedTextColor.DARK_GRAY)
                .append(Component.text("MiniGameCore").color(NamedTextColor.GOLD))
                .append(Component.text("] ").color(NamedTextColor.DARK_GRAY));
    }

    public ComponentFactory(String text) {
        this();
        this.component = component.append(Component.text(text));
    }

    public ComponentFactory(String text, NamedTextColor color) {
        this();
        this.component = component.append(Component.text(text).color(color));
    }

    public ComponentFactory addText(String text) {
        this.component = component.append(Component.text(text));
        return this;
    }

    public ComponentFactory addColorText(String text, NamedTextColor color) {
        this.component = component.append(Component.text(text).color(color));
        return this;
    }

    public ComponentFactory addColorText(String text, TextColor color) {
        this.component = component.append(Component.text(text).color(color));
        return this;
    }

    public TextComponent toComponent() {
        return component;
    }
}
