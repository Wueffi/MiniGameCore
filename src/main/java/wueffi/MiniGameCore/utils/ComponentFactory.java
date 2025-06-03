package wueffi.MiniGameCore.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

public class ComponentFactory {
    TextComponent.Builder component;

    public ComponentFactory() {
        component = Component.text();
        this.addColorText("[", NamedTextColor.DARK_GRAY);
        this.addColorText("MiniGameCore", NamedTextColor.GOLD);
        this.addColorText("] ", NamedTextColor.DARK_GRAY);
    }

    public ComponentFactory(String text) {
        this();
        this.addText(text);
    }

    public ComponentFactory(String text, NamedTextColor color) {
        this();
        this.addColorText(text, color);
    }

    public ComponentFactory addText(String text) {
        component.append(Component.text(text));
        return this;
    }

    public ComponentFactory addColorText(String text, NamedTextColor color) {
        component.append(Component.text(text).color(color));
        return this;
    }

    public ComponentFactory addColorText(String text, TextColor color) {
        component.append(Component.text(text).color(color));
        return this;
    }

    public TextComponent toComponent() {
        return component.build();
    }
}
