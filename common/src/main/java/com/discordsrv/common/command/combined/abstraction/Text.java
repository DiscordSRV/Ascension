package com.discordsrv.common.command.combined.abstraction;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.Arrays;
import java.util.EnumSet;

public class Text {

    private final String content;
    private TextColor gameColor;
    private EnumSet<TextDecoration> gameFormatting;
    private EnumSet<Formatting> discordFormatting;

    public Text(String content) {
        this.content = content;
        this.gameFormatting = EnumSet.noneOf(TextDecoration.class);
        this.discordFormatting = EnumSet.noneOf(Formatting.class);
    }

    public String content() {
        return content;
    }

    public Text withGameColor(TextColor color) {
        this.gameColor = color;
        return this;
    }

    public TextColor gameColor() {
        return gameColor;
    }

    public Text withFormatting(Formatting... formatting) {
        EnumSet<TextDecoration> game = EnumSet.noneOf(TextDecoration.class);
        for (Formatting format : formatting) {
            game.add(format.game);
        }

        this.gameFormatting = game;
        this.discordFormatting = EnumSet.copyOf(Arrays.asList(formatting));
        return this;
    }

    public Text withGameFormatting(Formatting... formatting) {
        EnumSet<TextDecoration> game = EnumSet.noneOf(TextDecoration.class);
        for (Formatting format : formatting) {
            game.add(format.game);
        }

        this.gameFormatting = game;
        return this;
    }

    public EnumSet<TextDecoration> gameFormatting() {
        return gameFormatting;
    }

    public Text withDiscordFormatting(Formatting... formatting) {
        this.discordFormatting = EnumSet.copyOf(Arrays.asList(formatting));
        return this;
    }

    public EnumSet<Formatting> discordFormatting() {
        return discordFormatting;
    }

    public enum Formatting {
        BOLD(TextDecoration.BOLD, "**"),
        ITALICS(TextDecoration.ITALIC, "*"),
        UNDERLINED(TextDecoration.UNDERLINED, "__"),
        STRIKETHROUGH(TextDecoration.STRIKETHROUGH, "~~");

        private final TextDecoration game;
        private final String discord;

        Formatting(TextDecoration game, String discord) {
            this.game = game;
            this.discord = discord;
        }

        public String discord() {
            return discord;
        }
    }
}
