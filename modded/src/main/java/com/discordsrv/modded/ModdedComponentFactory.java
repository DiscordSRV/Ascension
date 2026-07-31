/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.modded;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.core.component.ComponentFactory;
import com.discordsrv.common.util.ComponentUtil;
import com.google.common.base.Suppliers;
import com.discordsrv.unrelocate.com.google.gson.Gson;
import com.discordsrv.unrelocate.com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.JsonOps;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.function.Supplier;

public class ModdedComponentFactory extends ComponentFactory {

    //? if minecraft: > 1.20.4
    private final Supplier<HolderLookup.Provider> holderProvider;
    private Method parseMethod;

    public ModdedComponentFactory(ModdedDiscordSRV discordSRV) {
        super(discordSRV);

        //? if minecraft: > 1.20.4 {
        this.holderProvider = Suppliers.ofInstance(RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY));

        try {
            // Load the same classes as the Minecraft Server.
            Class<?> parserClass = net.minecraft.network.chat.ComponentSerialization.class.getClassLoader().loadClass("com.google.gs".concat("on.JsonParser"));
            this.parseMethod = parserClass.getMethod("parseString", String.class);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            logger.error("Failed to find JsonParser class or parse method. This will cause issues with component serialization.", e);
        }
        //?}
    }


    public Component fromNative(net.minecraft.network.chat.Component text) {
        return deserialize(text);
    }

    public Component toAdventure(net.minecraft.network.chat.Component text) {
        return fromNative(text);
    }

    public net.minecraft.network.chat.Component toNative(Component component) {
        return serialize(component);
    }

    public net.minecraft.network.chat.Component fromAdventure(Component component) {
        return toNative(component);
    }

    public MinecraftComponent toAPI(Component component) {
        return ComponentUtil.toAPI(component);
    }

    public MinecraftComponent toAPI(net.minecraft.network.chat.Component text) {
        return toAPI(fromNative(text));
    }

    public Audience audience(@NotNull CommandSourceStack source) {
        return new Audience() {
            @Override
            public void sendMessage(@NotNull Component message) {
                source.sendSystemMessage(serialize(message));
            }
        };
    }

    //? if minecraft: > 1.20.4 {
    // From the internals of adventure platform modcommon.
    private Component deserialize(final net.minecraft.network.chat.Component input) {
        JsonElement vanillaJson = (JsonElement) net.minecraft.network.chat.ComponentSerialization.CODEC
                .encodeStart(this.holderProvider.get().createSerializationContext(JsonOps.INSTANCE), input)
                .getOrThrow(JsonParseException::new);
        return GsonComponentSerializer.gson().deserialize(vanillaJson.toString());
    }

    private net.minecraft.network.chat.Component serialize(final Component component) {
        String jsonString = GsonComponentSerializer.gson().serialize(component);
        return net.minecraft.network.chat.ComponentSerialization.CODEC
                .decode(this.holderProvider.get().createSerializationContext(JsonOps.INSTANCE), parseFromString(jsonString))
                .getOrThrow(JsonParseException::new)
                .getFirst();
    }

    //? } else {
    /*private Component deserialize(final net.minecraft.network.chat.Component input) {
        String jsonString = com.discordsrv.modded.mixin.component.ComponentSerializerAccess.getGSON().toJson(input);
        return GsonComponentSerializer.gson().deserialize(jsonString);
    }

    private net.minecraft.network.chat.Component serialize(final Component component) {
        String jsonString = GsonComponentSerializer.gson().serialize(component);
        return com.discordsrv.modded.mixin.component.ComponentSerializerAccess.getGSON().fromJson(jsonString, net.minecraft.network.chat.Component.class);
    }
    *///? }

    private JsonElement parseFromString(String jsonString) {
        try {
            return (JsonElement) parseMethod.invoke(null, jsonString);
        } catch (Exception e) {
            logger.error("Failed to parse JSON string: " + jsonString, e);
            return null;
        }
    }
}
