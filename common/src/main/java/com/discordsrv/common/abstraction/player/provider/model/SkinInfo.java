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

package com.discordsrv.common.abstraction.player.provider.model;

import com.discordsrv.api.placeholder.annotation.Placeholder;
import com.discordsrv.api.placeholder.annotation.PlaceholderPrefix;

import java.net.URL;

@PlaceholderPrefix("skin_")
public class SkinInfo {

    private final String textureId;
    private final String model;
    private final Parts parts;

    public SkinInfo(URL textureUrl, String model, Parts parts) {
        String textureUrlPlain = textureUrl.toString();
        this.textureId = textureUrlPlain.substring(textureUrlPlain.lastIndexOf('/') + 1);
        this.model = model;
        this.parts = parts;
    }

    public SkinInfo(String textureId, String model, Parts parts) {
        this.textureId = textureId;
        this.model = model;
        this.parts = parts;
    }

    @Placeholder("texture_id")
    public String textureId() {
        return textureId;
    }

    @Placeholder("model")
    public String model() {
        return model;
    }

    @Placeholder("parts")
    public Parts getParts() {
        return parts;
    }

    @PlaceholderPrefix("skin_parts_")
    public static class Parts {

        private final boolean cape;
        private final boolean jacket;
        private final boolean leftSleeve;
        private final boolean rightSleeve;
        private final boolean leftPants;
        private final boolean rightPants;
        private final boolean hat;

        public Parts(boolean cape, boolean jacket, boolean leftSleeve, boolean rightSleeve, boolean leftPants, boolean rightPants, boolean hat) {
            this.cape = cape;
            this.jacket = jacket;
            this.leftSleeve = leftSleeve;
            this.rightSleeve = rightSleeve;
            this.leftPants = leftPants;
            this.rightPants = rightPants;
            this.hat = hat;
        }

        public Parts(int parts) {
            this(
                    (parts & 1) == 1,
                    ((parts >> 1) & 1) == 1,
                    ((parts >> 2) & 1) == 1,
                    ((parts >> 3) & 1) == 1,
                    ((parts >> 4) & 1) == 1,
                    ((parts >> 5) & 1) == 1,
                    ((parts >> 6) & 1) == 1
            );
        }

        @Placeholder("cape")
        public boolean isCape() {
            return cape;
        }

        @Placeholder("jacket")
        public boolean isJacket() {
            return jacket;
        }

        @Placeholder("left_sleeve")
        public boolean isLeftSleeve() {
            return leftSleeve;
        }

        @Placeholder("right_sleeve")
        public boolean isRightSleeve() {
            return rightSleeve;
        }

        @Placeholder("left_pants")
        public boolean isLeftPants() {
            return leftPants;
        }

        @Placeholder("right_pants")
        public boolean isRightPants() {
            return rightPants;
        }

        @Placeholder("hat")
        public boolean isHat() {
            return hat;
        }
    }
}
