/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.component;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.MockDiscordSRV;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.placeholder.PlaceholderServiceTest;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

public class GameTextBuilderTest {

    private final ComponentFactory componentFactory = MockDiscordSRV.getInstance().componentFactory();

    @Test
    public void placeholderTest() {
        MinecraftComponent apiComponent = componentFactory.textBuilder("%static_field%")
                .applyPlaceholderService()
                .addContext(PlaceholderServiceTest.BasicContext.class)
                .build();

        Component component = component(apiComponent);
        Assertions.assertNotNull(component);
        Assertions.assertTrue(any(component, componentContains(PlaceholderServiceTest.BasicContext.STATIC_FIELD)));
    }

    @Test
    public void unsafeTest() {
        MinecraftComponent apiComponent = componentFactory.textBuilder("%unsafe_input%")
                .applyPlaceholderService()
                .addContext(PlaceholderServiceTest.BasicContext.class)
                .build();

        Component component = component(apiComponent);

        Assertions.assertNotNull(component);
        Assertions.assertTrue(any(component, componentContains(PlaceholderServiceTest.BasicContext.UNSAFE_INPUT)));
    }

    private Component component(MinecraftComponent apiComponent) {
        return (Component) apiComponent.asAdventure(GsonComponentSerializer.class);
    }

    private boolean all(Component component, Predicate<Component> check) {
        if (!check.test(component)) {
            return false;
        }
        for (Component child : component.children()) {
            if (!all(child, check)) {
                return false;
            }
        }
        return true;
    }
    private boolean any(Component component, Predicate<Component> check) {
        if (check.test(component)) {
            return true;
        }
        for (Component child : component.children()) {
            if (any(child, check)) {
                return true;
            }
        }
        return false;
    }
    private Predicate<Component> componentContains(String content) {
        return component -> {
            if (!(component instanceof TextComponent)) {
                return false;
            }
            return ((TextComponent) component).content().contains(content);
        };
    }
}
