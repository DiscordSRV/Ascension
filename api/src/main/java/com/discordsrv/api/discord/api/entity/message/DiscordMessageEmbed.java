/*
 * This file is part of the DiscordSRV API, licensed under the MIT License
 * Copyright (c) 2016-2021 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.api.discord.api.entity.message;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Color;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A Discord embed.
 */
@SuppressWarnings("unused") // API
public class DiscordMessageEmbed {

    /**
     * Create a new builder for {@link DiscordMessageEmbed}s.
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    private final int color;
    private final String authorName, authorUrl, authorImageUrl;
    private final String title, titleUrl;
    private final String description;
    private final List<Field> fields;
    private final String thumbnailUrl;
    private final String imageUrl;
    private final OffsetDateTime timestamp;
    private final String footer, footerImageUrl;

    public DiscordMessageEmbed(MessageEmbed embed) {
        MessageEmbed.AuthorInfo author = embed.getAuthor();
        MessageEmbed.Thumbnail thumbnail = embed.getThumbnail();
        MessageEmbed.ImageInfo image = embed.getImage();
        MessageEmbed.Footer footer = embed.getFooter();

        List<Field> fields = new ArrayList<>();
        for (MessageEmbed.Field field : embed.getFields()) {
            fields.add(new Field(field.getName(), field.getValue(), field.isInline()));
        }

        this.color = embed.getColorRaw();
        this.authorName = author != null ? author.getName(): null;
        this.authorUrl = author != null ? author.getUrl() : null;
        this.authorImageUrl = author != null ? author.getIconUrl() : null;
        this.title = embed.getTitle();
        this.titleUrl = embed.getUrl();
        this.description = embed.getDescription();
        this.fields = fields;
        this.thumbnailUrl = thumbnail != null ? thumbnail.getUrl() : null;
        this.imageUrl = image != null ? image.getUrl() : null;
        this.timestamp = embed.getTimestamp();
        this.footer = footer != null ? footer.getText() : null;
        this.footerImageUrl = footer != null ? footer.getIconUrl() : null;
    }

    public DiscordMessageEmbed(int color, String authorName, String authorUrl, String authorAvatarUrl, String title,
                               String titleUrl, String description, List<Field> fields, String thumbnailUrl,
                               String imageUrl, OffsetDateTime timestamp, String footer, String footerImageUrl) {
        this.color = color;
        this.authorName = authorName;
        this.authorUrl = authorUrl;
        this.authorImageUrl = authorAvatarUrl;
        this.title = title;
        this.titleUrl = titleUrl;
        this.description = description;
        this.fields = fields;
        this.thumbnailUrl = thumbnailUrl;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.footer = footer;
        this.footerImageUrl = footerImageUrl;
    }

    public int getColor() {
        return color;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public String getAuthorImageUrl() {
        return authorImageUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getTitleUrl() {
        return titleUrl;
    }

    public String getDescription() {
        return description;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public String getFooter() {
        return footer;
    }

    public String getFooterImageUrl() {
        return footerImageUrl;
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public MessageEmbed toJDA() {
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setColor(color);
        embedBuilder.setAuthor(authorName, authorUrl, authorImageUrl);
        embedBuilder.setTitle(title, titleUrl);
        embedBuilder.setDescription(description);
        for (Field field : fields) {
            embedBuilder.addField(new MessageEmbed.Field(field.getTitle(), field.getValue(), field.isInline(), false));
        }
        embedBuilder.setThumbnail(thumbnailUrl);
        embedBuilder.setImage(imageUrl);
        embedBuilder.setTimestamp(timestamp);
        embedBuilder.setFooter(footer, footerImageUrl);
        return embedBuilder.build();
    }

    public static class Field {

        private final String title;
        private final String value;
        private final boolean inline;

        public Field(@Nullable CharSequence title, @Nullable CharSequence value, boolean inline) {
            this.title = title != null ? title.toString() : null;
            this.value = value != null ? value.toString() : null;
            this.inline = inline;
        }

        @Nullable
        public String getTitle() {
            return title;
        }

        @Nullable
        public String getValue() {
            return value;
        }

        public boolean isInline() {
            return inline;
        }
    }

    public static class Builder {

        private int color;
        private String authorName, authorUrl, authorImageUrl;
        private String title, titleUrl;
        private String description;
        private final List<Field> fields;
        private String thumbnailUrl;
        private String imageUrl;
        private OffsetDateTime timestamp;
        private String footer, footerImageUrl;

        protected Builder() {
            this.fields = new ArrayList<>();
        }

        protected Builder(int color, String authorName, String authorUrl, String authorImageUrl, String title,
                       String titleUrl, String description, List<Field> fields, String thumbnailUrl, String imageUrl,
                       OffsetDateTime timestamp, String footer, String footerImageUrl) {
            this.color = color;
            this.authorName = authorName;
            this.authorUrl = authorUrl;
            this.authorImageUrl = authorImageUrl;
            this.title = title;
            this.titleUrl = titleUrl;
            this.description = description;
            this.fields = new ArrayList<>(fields);
            this.thumbnailUrl = thumbnailUrl;
            this.imageUrl = imageUrl;
            this.timestamp = timestamp;
            this.footer = footer;
            this.footerImageUrl = footerImageUrl;
        }

        protected Builder(DiscordMessageEmbed embed) {
            this.color = embed.getColor();
            this.authorName = embed.getAuthorName();
            this.authorUrl = embed.getAuthorUrl();
            this.authorImageUrl = embed.getAuthorImageUrl();
            this.title = embed.getTitle();
            this.titleUrl = embed.getTitleUrl();
            this.description = embed.getDescription();
            this.fields = embed.getFields();
            this.thumbnailUrl = embed.getThumbnailUrl();
            this.imageUrl = embed.getImageUrl();
            this.timestamp = embed.getTimestamp();
            this.footer = embed.getFooter();
            this.footerImageUrl = embed.getFooterImageUrl();
        }

        public int getColor() {
            return color;
        }

        public Builder setColor(Color color) {
            return setColor(color.getRGB());
        }

        public Builder setColor(int color) {
            this.color = color;
            return this;
        }

        public Builder setAuthor(@Nullable CharSequence authorName, @Nullable CharSequence authorUrl) {
            return setAuthor(authorName, authorUrl, null);
        }

        public Builder setAuthor(@Nullable CharSequence authorName, @Nullable CharSequence authorUrl, @Nullable CharSequence authorImageUrl) {
            this.authorName = authorName != null ? authorName.toString() : null;
            this.authorUrl = authorUrl != null ? authorUrl.toString() : null;
            this.authorImageUrl = authorImageUrl != null ? authorImageUrl.toString() : null;
            return this;
        }

        public String getAuthorName() {
            return authorName;
        }

        public Builder setAuthorName(@Nullable CharSequence authorName) {
            this.authorName = authorName != null ? authorName.toString() : null;
            return this;
        }

        public String getAuthorUrl() {
            return authorUrl;
        }

        public Builder setAuthorUrl(@Nullable CharSequence authorUrl) {
            this.authorUrl = authorUrl != null ? authorUrl.toString() : null;
            return this;
        }

        public String getAuthorImageUrl() {
            return authorImageUrl;
        }

        public Builder setAuthorImageUrl(@Nullable CharSequence authorImageUrl) {
            this.authorImageUrl = authorImageUrl != null ? authorImageUrl.toString() : null;
            return this;
        }

        public Builder setTitle(@Nullable CharSequence title, @Nullable CharSequence titleUrl) {
            this.title = title != null ? title.toString() : null;
            this.titleUrl = titleUrl != null ? titleUrl.toString() : null;
            return this;
        }

        public String getTitle() {
            return title;
        }

        public Builder setTitle(@Nullable CharSequence title) {
            this.title = title != null ? title.toString() : null;
            return this;
        }

        public String getTitleUrl() {
            return titleUrl;
        }

        public Builder setTitleUrl(@Nullable CharSequence titleUrl) {
            this.titleUrl = titleUrl != null ? titleUrl.toString() : null;
            return this;
        }

        public String getDescription() {
            return description;
        }

        public Builder setDescription(@Nullable CharSequence description) {
            this.description = description != null ? description.toString() : null;
            return this;
        }

        public List<Field> getFields() {
            return fields;
        }

        public Builder addField(@NotNull CharSequence title, @NotNull CharSequence value, boolean inline) {
            return addField(new Field(title, value, inline));
        }

        public Builder addField(@NotNull Field field) {
            this.fields.add(field);
            return this;
        }

        public Builder removeField(@NotNull Field field) {
            this.fields.remove(field);
            return this;
        }

        public String getThumbnailUrl() {
            return thumbnailUrl;
        }

        public Builder setThumbnailUrl(@Nullable CharSequence thumbnailUrl) {
            this.thumbnailUrl = thumbnailUrl != null ? thumbnailUrl.toString() : null;
            return this;
        }

        public String getImageUrl() {
            return imageUrl;
        }

        public Builder setImageUrl(@Nullable CharSequence imageUrl) {
            this.imageUrl = imageUrl != null ? imageUrl.toString() : null;
            return this;
        }

        public OffsetDateTime getTimestamp() {
            return timestamp;
        }

        public Builder setTimestamp(@Nullable OffsetDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder setFooter(@Nullable CharSequence footer, @Nullable CharSequence footerImageUrl) {
            this.footer = footer != null ? footer.toString() : null;
            this.footerImageUrl = footerImageUrl != null ? footerImageUrl.toString() : null;
            return this;
        }

        public String getFooter() {
            return footer;
        }

        public Builder setFooter(@Nullable CharSequence footer) {
            this.footer = footer != null ? footer.toString() : null;
            return this;
        }

        public String getFooterImageUrl() {
            return footerImageUrl;
        }

        public Builder setFooterImageUrl(@Nullable CharSequence footerImageUrl) {
            this.footerImageUrl = footerImageUrl != null ? footerImageUrl.toString() : null;
            return this;
        }

        public DiscordMessageEmbed build() {
            return new DiscordMessageEmbed(color, authorName, authorUrl, authorImageUrl, title, titleUrl, description,
                    fields, thumbnailUrl, imageUrl, timestamp, footer, footerImageUrl);
        }

        @SuppressWarnings({"MethodDoesntCallSuperMethod", "CloneDoesntDeclareCloneNotSupportedException"})
        @Override
        public Builder clone() {
            return new Builder(color, authorName, authorUrl, authorImageUrl, title, titleUrl, description,
                    fields, thumbnailUrl, imageUrl, timestamp, footer, footerImageUrl);
        }
    }
}
