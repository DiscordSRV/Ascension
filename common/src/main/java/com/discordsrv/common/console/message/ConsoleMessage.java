package com.discordsrv.common.console.message;

import com.discordsrv.common.DiscordSRV;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.*;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
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
                    + "(?<ansi>[0-9]{1,3}"
                    + "(;[0-9]{1,3}"
                    + "(;[0-9]{1,3}"
                    + "(?:(?:;[0-9]{1,3}){2})?"
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
        return discordSRV.componentFactory().ansiSerializer().serialize(component) + (ANSI_ESCAPE + "[0m");
    }

    public String asPlain() {
        Component component = builder.build();
        return discordSRV.componentFactory().plainSerializer().serialize(component);
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
        String[] ansiParts = ansiEscape.split(";");
        int amount = ansiParts.length;
        if (amount == 1 || amount == 2) {
            int number = Integer.parseInt(ansiParts[0]);

            if ((number >= 30 && number <= 37) || (number >= 90 && number <= 97)) {
                style.color(fourBitAnsiColor(number));
                return;
            }

            switch (number) {
                case 0:
                    style.color(null).decorations(EnumSet.allOf(TextDecoration.class), false);
                    break;
                case 1:
                    style.decoration(TextDecoration.BOLD, true);
                    break;
                case 3:
                    style.decoration(TextDecoration.ITALIC, true);
                    break;
                case 4:
                    style.decoration(TextDecoration.UNDERLINED, true);
                    break;
                case 8:
                    style.decoration(TextDecoration.OBFUSCATED, true);
                    break;
                case 9:
                    style.decoration(TextDecoration.STRIKETHROUGH, true);
                    break;
                case 22:
                    style.decoration(TextDecoration.BOLD, false);
                    break;
                case 23:
                    style.decoration(TextDecoration.ITALIC, false);
                    break;
                case 24:
                    style.decoration(TextDecoration.UNDERLINED, false);
                    break;
                case 28:
                    style.decoration(TextDecoration.OBFUSCATED, false);
                    break;
                case 29:
                    style.decoration(TextDecoration.STRIKETHROUGH, false);
                    break;
                case 39:
                    style.color(null);
                    break;
            }
        } else if (amount == 3 || amount == 5) {
            if (Integer.parseInt(ansiParts[0]) != 36 || Integer.parseInt(ansiParts[1]) != 5) {
                return;
            }

            if (amount == 5)  {
                int red = Integer.parseInt(ansiParts[2]);
                int green = Integer.parseInt(ansiParts[3]);
                int blue = Integer.parseInt(ansiParts[4]);

                style.color(TextColor.color(red, green, blue));
                return;
            }

            int number = Integer.parseInt(ansiParts[2]);
            style.color(eightBitAnsiColor(number));
        }
    }

    private enum FourBitColor {
        BLACK(30, TextColor.color(0, 0, 0)),
        RED(31, TextColor.color(170, 0, 0)),
        GREEN(32, TextColor.color(0, 170, 0)),
        YELLOW(33, TextColor.color(170, 85, 0)),
        BLUE(34, TextColor.color(0, 0, 170)),
        MAGENTA(35, TextColor.color(170, 0, 170)),
        CYAN(36, TextColor.color(0, 170, 170)),
        WHITE(37, TextColor.color(170, 170, 170)),
        BRIGHT_BLACK(90, TextColor.color(85, 85, 85)),
        BRIGHT_RED(91, TextColor.color(255, 85, 85)),
        BRIGHT_GREEN(92, TextColor.color(85, 255, 85)),
        BRIGHT_YELLOW(93, TextColor.color(255, 255, 85)),
        BRIGHT_BLUE(94, TextColor.color(85, 85, 255)),
        BRIGHT_MAGENTA(95, TextColor.color(255, 85, 255)),
        BRIGHT_CYAN(96, TextColor.color(85, 255, 255)),
        BRIGHT_WHITE(97, TextColor.color(255, 255, 255));

        private static final Map<Integer, FourBitColor> byFG = new HashMap<>();

        static {
            for (FourBitColor value : values()) {
                byFG.put(value.fg, value);
            }
        }

        public static FourBitColor getByFG(int fg) {
            return byFG.get(fg);
        }

        private final int fg;
        private final TextColor color;

        FourBitColor(int fg, TextColor color) {
            this.fg = fg;
            this.color = color;
        }

        public TextColor color() {
            return color;
        }
    }

    private TextColor fourBitAnsiColor(int color) {
        FourBitColor fourBitColor = FourBitColor.getByFG(color);
        return fourBitColor != null ? fourBitColor.color() : null;
    }

    private TextColor[] colors;
    private TextColor eightBitAnsiColor(int color) {
        if (colors == null) {
            TextColor[] colors = new TextColor[256];

            FourBitColor[] fourBitColors = FourBitColor.values();
            for (int i = 0; i < fourBitColors.length; i++) {
                colors[i] = fourBitColors[i].color();
            }

            // https://gitlab.gnome.org/GNOME/vte/-/blob/19acc51708d9e75ef2b314aa026467570e0bd8ee/src/vte.cc#L2485
            for (int i = 16; i < 232; i++) {
                int j = i - 16;

                int red = j / 36;
                int green = (j / 6) % 6;
                int blue = j % 6;

                red = red == 0 ? 0 : red * 40 + 55;
                green = green == 0 ? 0 : green * 40 + 55;
                blue = blue == 0 ? 0 : blue * 40 + 55;

                colors[i] = TextColor.color(
                        red | red << 8,
                        green | green << 8,
                        blue | blue << 8
                );
            }
            for (int i = 232; i < 256; i++) {
                int shade = 8 + (i - 232) * 10;
                colors[i] = TextColor.color(shade, shade, shade);
            }

            this.colors = colors;
        }

        return color <= colors.length && color >= 0 ? colors[color] : null;
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
