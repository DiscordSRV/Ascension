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
