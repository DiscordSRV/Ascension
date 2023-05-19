/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2023 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
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

package com.discordsrv.common.update;

import com.discordsrv.api.event.bus.Subscribe;
import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.connection.ConnectionConfig;
import com.discordsrv.common.config.connection.UpdateConfig;
import com.discordsrv.common.debug.data.VersionInfo;
import com.discordsrv.common.exception.MessageException;
import com.discordsrv.common.logging.NamedLogger;
import com.discordsrv.common.player.IPlayer;
import com.discordsrv.common.player.event.PlayerConnectedEvent;
import com.discordsrv.common.update.github.GitHubCompareResponse;
import com.discordsrv.common.update.github.GithubRelease;
import com.fasterxml.jackson.core.type.TypeReference;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UpdateChecker {

    private static final String USER_DOWNLOAD_URL = "-";// "https://discordsrv.com/download";
    private static final String GITHUB_REPOSITORY = "DiscordSRV/Ascension";
    private static final String GITHUB_DEV_BRANCH = "main";

    private static final String DOWNLOAD_SERVICE_HOST = "https://download.discordsrv.com";
    private static final String DOWNLOAD_SERVICE_SNAPSHOT_CHANNEL = "testing";
    private static final String DOWNLOAD_SERVICE_RELEASE_CHANNEL = null;

    private static final String GITHUB_API_HOST = "https://api.github.com";

    private final DiscordSRV discordSRV;
    private final NamedLogger logger;

    private boolean securityFailed = false;
    private VersionCheck latestCheck;
    private VersionCheck loggedCheck;

    public UpdateChecker(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
        this.logger = new NamedLogger(discordSRV, "UPDATES");
        discordSRV.eventBus().subscribe(this);
    }

    public boolean isSecurityFailed() {
        return securityFailed;
    }

    /**
     * @return if enabling is permitted
     */
    public boolean check(boolean logUpToDate) {
        UpdateConfig config = discordSRV.connectionConfig().update;
        boolean isSnapshot = discordSRV.versionInfo().isSnapshot();
        boolean isSecurity = config.security.enabled;
        boolean isFirstPartyNotification = config.firstPartyNotification;
        boolean isNotification = config.notificationEnabled;

        if (!isSecurity && !isNotification) {
            // Nothing to do
            return true;
        }

        if (isFirstPartyNotification || isSecurity) {
            VersionCheck check = null;
            try {
                check = checkFirstParty(isSnapshot);
                if (check == null && isSecurity) {
                    securityFailed = true;
                    return false;
                }
            } catch (Throwable t) {
                List<String> failedThings = new ArrayList<>(2);
                if (isSecurity) {
                    failedThings.add("perform version security check");
                }
                if (isFirstPartyNotification) {
                    failedThings.add("check for updates");
                }
                logger.warning("Failed to " + String.join(" and ", failedThings)
                                       + " from the first party API", t);

                if (config.security.force) {
                    logger.error("Security check is required (as configured in " + ConnectionConfig.FILE_NAME + ")"
                                         + ", startup will be cancelled.");
                    securityFailed = true;
                    return false;
                }
            }

            if (isNotification && isFirstPartyNotification
                    && check != null && check.status != VersionCheck.Status.UNKNOWN) {
                latestCheck = check;
                log(check, logUpToDate);
                return true;
            }
        }

        if (isNotification && config.github.enabled) {
            VersionCheck check = null;
            try {
                check = checkGitHub(isSnapshot);
            } catch (Throwable t) {
                logger.warning("Failed to check for updates from GitHub", t);
            }

            if (check != null && check.status != VersionCheck.Status.UNKNOWN) {
                latestCheck = check;
                log(check, logUpToDate);
                return true;
            } else {
                logger.error("Update check" + (isFirstPartyNotification ? "s" : "") + " failed: unknown version");
            }
        }

        return true;
    }

    private ResponseBody checkResponse(Request request, Response response, ResponseBody responseBody) throws IOException {
        if (responseBody == null || !response.isSuccessful()) {
            String responseString = responseBody == null
                             ? "response body is null"
                             : StringUtils.substring(responseBody.string(), 0, 500);
            throw new MessageException("Request to " + request.url().host() + " failed: " + response.code() + ": " + responseString);
        }
        return responseBody;
    }

    /**
     * @return {@code null} for preventing shutdown
     */
    private VersionCheck checkFirstParty(boolean isSnapshot) throws IOException {
        VersionInfo versionInfo = discordSRV.versionInfo();
        Request request = new Request.Builder()
                .url(DOWNLOAD_SERVICE_HOST + "/v2/" + GITHUB_REPOSITORY
                             + "/" + (isSnapshot ? DOWNLOAD_SERVICE_SNAPSHOT_CHANNEL : DOWNLOAD_SERVICE_RELEASE_CHANNEL)
                             + "/version-check/" + (isSnapshot ? versionInfo.gitRevision() : versionInfo.version()))
                .get().build();

        String responseString;
        try (Response response = discordSRV.httpClient().newCall(request).execute()) {
            ResponseBody responseBody = checkResponse(request, response, response.body());
            responseString = responseBody.string();
        }

        VersionCheck versionCheck = discordSRV.json().readValue(responseString, VersionCheck.class);
        if (versionCheck == null) {
            throw new MessageException("Failed to parse " + request.url() + " response body: " + StringUtils.substring(responseString, 0, 500));
        }

        boolean insecure = versionCheck.insecure;
        List<String> securityIssues = versionCheck.securityIssues;
        if (insecure) {
            logger.error("This version of DiscordSRV is insecure, security issues are listed below, startup will be cancelled.");
            for (String securityIssue : versionCheck.securityIssues) {
                logger.error(securityIssue);
            }

            // Block startup
            return null;
        } else if (securityIssues != null && !securityIssues.isEmpty()) {
            logger.warning("There are security warnings for this version of DiscordSRV, listed below");
            for (String securityIssue : versionCheck.securityIssues) {
                logger.warning(securityIssue);
            }
        }

        return versionCheck;
    }

    private VersionCheck checkGitHub(boolean isSnapshot) throws IOException {
        VersionInfo versionInfo = discordSRV.versionInfo();
        if (isSnapshot) {
            Request request = new Request.Builder()
                    .url(GITHUB_API_HOST + "/repos/" + GITHUB_REPOSITORY + "/compare/"
                                 + GITHUB_DEV_BRANCH + "..." + versionInfo.gitRevision() + "?per_page=0")
                    .get().build();

            try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                ResponseBody responseBody = checkResponse(request, response, response.body());
                GitHubCompareResponse compare = discordSRV.json().readValue(responseBody.byteStream(), GitHubCompareResponse.class);

                VersionCheck versionCheck = new VersionCheck();
                versionCheck.amount = compare.behind_by;
                versionCheck.amountType = (compare.behind_by == 1 ? "commit" : "commits");
                versionCheck.amountSource = VersionCheck.AmountSource.GITHUB;
                if ("behind".equals(compare.status)) {
                    versionCheck.status = VersionCheck.Status.OUTDATED;
                } else if ("identical".equals(compare.status)) {
                    versionCheck.status = VersionCheck.Status.UP_TO_DATE;
                } else {
                    versionCheck.status = VersionCheck.Status.UNKNOWN;
                }

                return versionCheck;
            }
        }

        String version = versionInfo.version();

        int versionsBehind = 0;
        boolean found = false;

        int perPage = 100;
        int page = 0;
        for (int i = 0; i < 3 /* max 3 loops */; i++) {
            Request request = new Request.Builder()
                    .url(GITHUB_API_HOST + "/repos/" + GITHUB_REPOSITORY + "/releases?per_page=" + perPage + "&page=" + page)
                    .get().build();

            try (Response response = discordSRV.httpClient().newCall(request).execute()) {
                ResponseBody responseBody = checkResponse(request, response, response.body());
                List<GithubRelease> releases = discordSRV.json().readValue(responseBody.byteStream(), new TypeReference<List<GithubRelease>>() {});

                for (GithubRelease release : releases) {
                    if (version.equals(release.tag_name)) {
                        found = true;
                        break;
                    }
                    versionsBehind++;
                }
                if (found || releases.size() < perPage) {
                    break;
                }
            }
        }

        VersionCheck versionCheck = new VersionCheck();
        versionCheck.amountSource = VersionCheck.AmountSource.GITHUB;
        versionCheck.amountType = (versionsBehind == 1 ? "release" : "releases");
        if (!found) {
            versionCheck.status = VersionCheck.Status.UNKNOWN;
            versionCheck.amount = -1;
        } else {
            versionCheck.status = versionsBehind == 0
                                  ? VersionCheck.Status.UP_TO_DATE
                                  : VersionCheck.Status.OUTDATED;
            versionCheck.amount = versionsBehind;
        }
        return versionCheck;
    }

    private void log(VersionCheck versionCheck, boolean logUpToDate) {
        switch (versionCheck.status) {
            case UP_TO_DATE: {
                if (logUpToDate) {
                    logger.info("DiscordSRV is up-to-date.");
                    loggedCheck = versionCheck;
                }
                break;
            }
            case OUTDATED: {
                // only log if there is new information
                if (loggedCheck == null || loggedCheck.amount != versionCheck.amount) {
                    logger.warning(
                            "DiscordSRV is outdated by " + versionCheck.amount + " " + versionCheck.amountType
                                    + ". Get the latest version from https://discordsrv.com/dowload");
                    loggedCheck = versionCheck;
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected version check status: " + versionCheck.status.name());
        }
    }

    @Subscribe
    public void onPlayerConnected(PlayerConnectedEvent event) {
        UpdateConfig config = discordSRV.connectionConfig().update;
        if (!config.notificationEnabled || !config.notificationInGame) {
            return;
        }

        if (latestCheck == null || latestCheck.status != VersionCheck.Status.OUTDATED) {
            return;
        }

        IPlayer player = event.player();
        if (!player.hasPermission("discordsrv.updatenotification")) {
            return;
        }

        player.sendMessage(
                Component.text("There is a new version of DiscordSRV available, ", NamedTextColor.AQUA)
                        .append(Component.text()
                                        .content("click here to download it")
                                        .clickEvent(ClickEvent.openUrl(USER_DOWNLOAD_URL)))
        );
    }
}
