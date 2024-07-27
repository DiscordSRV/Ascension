/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2024 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.discordsrv.common.core.placeholder.format;

import com.discordsrv.api.placeholder.format.PlainPlaceholderFormat;
import dev.vankka.mcdiscordreserializer.rules.DiscordMarkdownRules;
import dev.vankka.mcdiscordreserializer.rules.StyleNode;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.TextNode;
import dev.vankka.simpleast.core.parser.Parser;
import dev.vankka.simpleast.core.parser.Rule;
import dev.vankka.simpleast.core.simple.SimpleMarkdownRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DiscordMarkdownFormatImpl implements PlainPlaceholderFormat {

    private final Parser<Object, Node<Object>, Object> parser;

    public DiscordMarkdownFormatImpl() {
        List<Rule<Object, Node<Object>, Object>> rules = new ArrayList<>();
        rules.add(SimpleMarkdownRules.createEscapeRule());
        rules.add(SimpleMarkdownRules.createNewlineRule());
        rules.add(DiscordMarkdownRules.createCodeBlockRule());
        rules.add(DiscordMarkdownRules.createCodeStringRule());
        rules.add(DiscordMarkdownRules.createSpecialTextRule());
        this.parser = new Parser<>().addRules(rules);
    }

    @Override
    public String map(String input, Function<String, String> placeholders) {
        List<Node<Object>> nodes = parser.parse(input);

        StringBuilder finalText = new StringBuilder();
        StringBuilder text = new StringBuilder();
        for (Node<Object> node : nodes) {
            if (node instanceof TextNode) {
                text.append(((TextNode<Object>) node).getContent());
            } else if (node instanceof StyleNode) {
                String content = text.toString();
                text.setLength(0);
                PlainPlaceholderFormat.with(Formatting.DISCORD, () -> finalText.append(placeholders.apply(content)));

                for (Object style : ((StyleNode<?, ?>) node).getStyles()) {
                    if (!(style instanceof StyleNode.Style)) {
                        continue;
                    }

                    StyleNode.Style textStyle = (StyleNode.Style) style;
                    String childText = ((TextNode<?>) node.getChildren().get(0)).getContent();

                    if (textStyle == StyleNode.Styles.CODE_STRING) {
                        String blockContent = placeholders.apply(childText);
                        if (blockContent != null && !blockContent.isEmpty()) {
                            finalText.append("`").append(blockContent).append("`");
                        }
                    } else if (textStyle instanceof StyleNode.CodeBlockStyle) {
                        String language = ((StyleNode.CodeBlockStyle) textStyle).getLanguage();

                        if (language != null && language.equals("ansi")) {
                            String blockContent = PlainPlaceholderFormat.supplyWith(Formatting.ANSI, () -> placeholders.apply(childText));
                            if (blockContent != null && !blockContent.isEmpty()) {
                                finalText.append("```ansi\n").append(blockContent).append("```");
                            }
                        } else {
                            String blockContent = PlainPlaceholderFormat.supplyWith(Formatting.PLAIN, () -> placeholders.apply(childText));
                            if (blockContent != null && !blockContent.isEmpty()) {
                                finalText
                                        .append("```")
                                        .append(language != null ? language : "")
                                        .append("\n")
                                        .append(blockContent)
                                        .append("```");
                            }
                        }
                    }
                }
            }
        }

        finalText.append(placeholders.apply(text.toString()));
        return finalText.toString();
    }

}
