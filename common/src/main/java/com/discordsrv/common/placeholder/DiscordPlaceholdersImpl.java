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

package com.discordsrv.common.placeholder;

import com.discordsrv.api.placeholder.PlainPlaceholderFormat;
import dev.vankka.mcdiscordreserializer.rules.DiscordMarkdownRules;
import dev.vankka.simpleast.core.TextStyle;
import dev.vankka.simpleast.core.node.Node;
import dev.vankka.simpleast.core.node.StyleNode;
import dev.vankka.simpleast.core.node.TextNode;
import dev.vankka.simpleast.core.parser.Parser;
import dev.vankka.simpleast.core.parser.Rule;
import dev.vankka.simpleast.core.simple.SimpleMarkdownRules;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class DiscordPlaceholdersImpl implements PlainPlaceholderFormat {

    private final Parser<Object, Node<Object>, Object> parser;

    public DiscordPlaceholdersImpl() {
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
                    if (!(style instanceof TextStyle)) {
                        continue;
                    }

                    TextStyle textStyle = (TextStyle) style;
                    String childText = ((TextNode<?>) node.getChildren().get(0)).getContent();

                    if (textStyle.getType() == TextStyle.Type.CODE_STRING) {
                        finalText.append("`").append(placeholders.apply(childText)).append("`");
                    } else if (textStyle.getType() == TextStyle.Type.CODE_BLOCK) {
                        String language = textStyle.getExtra().get("language");

                        if (language != null && language.equals("ansi")) {
                            PlainPlaceholderFormat.with(Formatting.ANSI, () -> finalText
                                    .append("```ansi\n")
                                    .append(placeholders.apply(childText))
                                    .append("```")
                            );
                        } else {
                            finalText
                                    .append("```")
                                    .append(language != null ? language : "")
                                    .append("\n")
                                    .append(placeholders.apply(childText))
                                    .append("```");
                        }
                    }
                }
            }
        }

        finalText.append(placeholders.apply(text.toString()));
        return finalText.toString();
    }

}
