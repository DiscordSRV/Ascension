package com.discordsrv.common.console.message;

import com.discordsrv.common.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.*;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleMessage {

    private static final String ANSI_ESCAPE = "\u001B";
    // Paper uses 007F as an intermediary
    private static final String SECTION = "[§\u007F]";

    // Regex pattern for matching ANSI + Legacy
    private static final Pattern PATTERN = Pattern.compile(
            // ANSI
            ANSI_ESCAPE
                    + "\\["
                    + "(?<ansi>\\d{1,3}"
                    + "(;\\d{1,3}"
                    + "(;\\d{1,3}"
                    + "(?:(?:;\\d{1,3}){2})?"
                    + ")?"
                    + ")?"
                    + ")"
                    + "m"
                    + "|"
                    + "(?<legacy>"
                    // Legacy color/formatting
                    + "(?:" + SECTION + "[0-9a-fk-or])"
                    + "|"
                    // Bungee/Spigot legacy
                    + "(?:" + SECTION + "x(?:" + SECTION + "[0-9a-f]){6})"
                    + ")"
    );

    private final TextComponent.Builder builder = Component.text();
    private final DiscordSRV discordSRV;

    public ConsoleMessage(DiscordSRV discordSRV, String input) {
        this.discordSRV = discordSRV;
        parse(input);
    }

    public String asMarkdown() {
        Component component = builder.build();
        return discordSRV.componentFactory().discordSerializer().serialize(component);
    }

    public String asAnsi() {
        Component component = builder.build();
        return discordSRV.componentFactory().ansiSerializer().serialize(component);
    }

    private void parse(String input) {
        Matcher matcher = PATTERN.matcher(input);

        Style.Builder style = Style.style();

        int lastMatchEnd = 0;
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            if (start != lastMatchEnd) {
                builder.append(Component.text(input.substring(lastMatchEnd, start), style.build()));
            }

            String ansi = matcher.group("ansi");
            if (ansi != null) {
                parseAnsi(ansi, style);
            }

            String legacy = matcher.group("legacy");
            if (legacy != null) {
                parseLegacy(legacy, style);
            }

            lastMatchEnd = end;
        }

        int length = input.length();
        if (lastMatchEnd != length) {
            builder.append(Component.text(input.substring(lastMatchEnd, length), style.build()));
        }
    }

    private void parseAnsi(String ansiEscape, Style.Builder style) {
        // TODO: implement
    }

    private void parseLegacy(String legacy, Style.Builder style) {
        if (legacy.length() == 2) {
            char character = legacy.toCharArray()[1];
            if (character == 'r') {
                style.color(null).decorations(EnumSet.allOf(TextDecoration.class), false);
            } else {
                TextFormat format = getFormat(character);
                if (format instanceof TextColor) {
                    style.color((TextColor) format);
                } else if (format instanceof TextDecoration) {
                    style.decorate((TextDecoration) format);
                }
            }
        } else {
            char[] characters = legacy.toCharArray();
            StringBuilder hex = new StringBuilder(7).append(TextColor.HEX_PREFIX);
            for (int i = 2; i < characters.length; i += 2) {
                hex.append(characters[i]);
            }
            style.color(TextColor.fromHexString(hex.toString()));
        }
    }

    private TextFormat getFormat(char character) {
        switch (character) {
            case '0': return NamedTextColor.BLACK;
            case '1': return NamedTextColor.DARK_BLUE;
            case '2': return NamedTextColor.DARK_GREEN;
            case '3': return NamedTextColor.DARK_AQUA;
            case '4': return NamedTextColor.DARK_RED;
            case '5': return NamedTextColor.DARK_PURPLE;
            case '6': return NamedTextColor.GOLD;
            case '7': return NamedTextColor.GRAY;
            case '8': return NamedTextColor.DARK_GRAY;
            case '9': return NamedTextColor.BLUE;
            case 'a': return NamedTextColor.GREEN;
            case 'b': return NamedTextColor.AQUA;
            case 'c': return NamedTextColor.RED;
            case 'd': return NamedTextColor.LIGHT_PURPLE;
            case 'e': return NamedTextColor.YELLOW;
            case 'f': return NamedTextColor.WHITE;
            case 'k': return TextDecoration.OBFUSCATED;
            case 'l': return TextDecoration.BOLD;
            case 'm': return TextDecoration.STRIKETHROUGH;
            case 'n': return TextDecoration.UNDERLINED;
            case 'o': return TextDecoration.ITALIC;
            default: return null;
        }
    }
}
