/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2026 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.discordsrv.api.configurate;

import com.discordsrv.api.color.Color;
import com.discordsrv.api.configurate.serializer.ColorSerializer;
import com.discordsrv.api.configurate.serializer.DiscordMessageEmbedSerializer;
import com.discordsrv.api.configurate.serializer.SendableDiscordMessageSerializer;
import com.discordsrv.api.configurate.serializer.SendableDiscordMessageTemplateSerializer;
import com.discordsrv.api.discord.entity.message.DiscordMessageEmbed;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessageTemplate;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.util.NamingScheme;
import org.spongepowered.configurate.util.NamingSchemes;

public class DiscordSRVConfigurate {

    public static final NamingScheme NAMING_SCHEME = in -> {
        // lower-case-dashed, but with no delimiter even if the first character is upper-case
        in = Character.toLowerCase(in.charAt(0)) + in.substring(1);
        in = NamingSchemes.LOWER_CASE_DASHED.coerce(in);
        return in;
    };

    public static final TypeSerializerCollection SERIALIZERS = TypeSerializerCollection.builder()
            .register(Color.class, new ColorSerializer())
            .register(DiscordMessageEmbed.Builder.class, new DiscordMessageEmbedSerializer())
            .register(DiscordMessageEmbed.Field.class, new DiscordMessageEmbedSerializer.FieldSerializer())
            .register(SendableDiscordMessage.Builder.class, new SendableDiscordMessageSerializer(false))
            .register(SendableDiscordMessageTemplate.class, new SendableDiscordMessageTemplateSerializer())
            .build();

    public static final ThreadLocal<Boolean> GENERATING_DEFAULT_CONFIG = ThreadLocal.withInitial(() -> false);
}
