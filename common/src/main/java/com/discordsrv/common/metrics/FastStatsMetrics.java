package com.discordsrv.common.metrics;

import com.discordsrv.common.DiscordSRV;
import com.discordsrv.common.config.main.ChannelUpdaterConfig;
import com.discordsrv.common.config.main.channels.base.BaseChannelConfig;
import com.discordsrv.common.config.main.channels.base.IChannelConfig;
import com.discordsrv.common.config.main.generic.DestinationConfig;
import com.discordsrv.common.config.main.sync.GroupSyncConfig;
import com.discordsrv.common.core.module.type.PluginIntegration;
import com.discordsrv.common.feature.linking.requirelinking.RequiredLinkingModule;
import dev.faststats.*;
import dev.faststats.data.Metric;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FastStatsMetrics {

    public static final ErrorTracker ERROR_TRACKER = ErrorTracker.contextAware()
        .addExclusionRule((thread, error) -> Arrays.stream(error.getStackTrace())
            .noneMatch(e -> e.getClassName().startsWith("com.discordsrv")));

    @Token
    public static final String TOKEN = "replace_with_discordsrv_token";
    private final DiscordSRV discordSRV;

    public FastStatsMetrics(DiscordSRV discordSRV) {
        this.discordSRV = discordSRV;
    }

    public FastStatsContext create(final SimpleContext.Factory<? extends SimpleContext, ? extends SimpleContext.Factory<?, ?>> platformFactory) {
        return platformFactory
                .errorTrackerService(ERROR_TRACKER)
                .metrics(factory -> {
                    factory.addMetric(Metric.string("version", () -> discordSRV.versionInfo().version()));
                    factory.addMetric(Metric.string("git_revision", () -> discordSRV.versionInfo().gitRevision()));
                    factory.addMetric(Metric.string("online_mode", () -> discordSRV.onlineMode().name().toLowerCase(Locale.ROOT)));
                    factory.addMetric(Metric.string("server_language", () -> discordSRV.defaultLocale().toLanguageTag()));
                    factory.addMetric(Metric.number("players_online", () -> discordSRV.playerProvider().allPlayers().size()));
                    factory.addMetric(Metric.string("storage_backend", () -> discordSRV.connectionConfig().storage.backend.prettyName()));
                    factory.addMetric(Metric.string("avatar_service_mode", () -> discordSRV.config().avatarProvider.avatarServiceMode.name()));
                    factory.addMetric(Metric.number("platform_plugins_count", () -> discordSRV.pluginManager().getPlugins().size()));

                    factory.addMetric(Metric.number("configured_channels", () -> discordSRV.channelConfig().getKeys().size()));
                    factory.addMetric(Metric.number("configured_destinations", () -> discordSRV.channelConfig().getAllChannels().stream()
                            .mapToInt(FastStatsMetrics::destinationCount)
                            .sum()));

                    factory.addMetric(Metric.booleanMap("integrations", () -> {
                        Map<String, Boolean> map = new HashMap<>();
                        for (PluginIntegration<?> integration : discordSRV.getModules(PluginIntegration.class, false)) {
                            map.put(integration.getIntegrationId(), true);
                        }
                        return map;
                    }));

                    factory.addMetric(Metric.number("linked_accounts", () -> {
                        var linkProvider = discordSRV.linkProvider();
                        return linkProvider != null ? linkProvider.store().getLinkedAccountCount().join() : 0;
                    }));
                    factory.addMetric(Metric.string("account_linking_provider", () -> {
                        var linkProvider = discordSRV.linkProvider();
                        return switch (discordSRV.config().linkedAccounts.provider) {
                            case AUTO -> linkProvider != null ? linkProvider.usesLocalLinking() ? "minecraftauth" : "storage" : "disabled";
                            case MINECRAFTAUTH -> discordSRV.connectionConfig().minecraftAuth.allow ? "minecraftauth" : "disabled";
                            case STORAGE -> "storage";
                        };
                    }));

                    factory.addMetric(Metric.booleanMap("features", () -> {
                        Map<String, Boolean> map = new HashMap<>();
                        // Chat features
                        if (discordSRV.channelConfig().getAllChannels().stream().anyMatch((channel) -> channel.minecraftToDiscord.enabled)) map.put("Minecraft -> Discord chat", true);
                        if (discordSRV.channelConfig().getAllChannels().stream().anyMatch((channel) -> channel.discordToMinecraft.enabled)) map.put("Discord -> Minecraft chat", true);
                        if (discordSRV.config().console.stream().anyMatch(config -> config.channel.channelId != 0L)) map.put("Console channel", true);

                        // Sync features
                        if (discordSRV.config().groupSync.getEntries().stream().anyMatch(GroupSyncConfig.Entry::isSet)) map.put("Group -> role synchronization", true);
                        if (discordSRV.config().nicknameSync.isSet()) map.put("Nickname synchronization", true);
                        if (discordSRV.config().banSync.isSet()) map.put("Ban synchronization", true);
                        if (discordSRV.config().muteSync.isSet()) map.put("Mute synchronization", true);
                        if (discordSRV.config().linkedRole.roleIds.stream().anyMatch(roleId -> roleId != 0L)) map.put("Linked roles", true);
                        if (discordSRV.config().onlineRole.syncConfig().isSet()) map.put("Online role", true);

                        // Other features
                        if (discordSRV.config().linkedAccounts.enabled) map.put("Account linking", true);
                        if (discordSRV.config().presenceUpdater.enabled) map.put("Presence updater", true);
                        if (discordSRV.config().channelUpdater.getConfigs().stream().anyMatch(ChannelUpdaterConfig.UpdaterConfig::any)) map.put("Channel updater", true);
                        if (discordSRV.config().customCommands.stream().anyMatch(command -> command.enabled)) map.put("Custom commands", true);
                        if (discordSRV.getModule(RequiredLinkingModule.class) != null) map.put("Require linked account to play", true);
                        if (discordSRV.config().rewards.linkingRewards.stream().anyMatch(reward -> !reward.consoleCommandsToRun.isEmpty())) map.put("Linking rewards", true);
                        if (discordSRV.config().rewards.boostingRewards.stream().anyMatch(reward -> reward.serverId != 0L && !reward.consoleCommandsToRun.isEmpty())) map.put("Boosting rewards", true);

                        return map;
                    }));

                    return factory.create();
                })
                .create();
    }

    private static int destinationCount(BaseChannelConfig config) {
        if (!(config instanceof IChannelConfig)) {
            return 0;
        }

        DestinationConfig destination = ((IChannelConfig) config).destination();
        int channelCount = destination.channelIds == null ? 0 : (int) destination.channelIds.stream()
                .filter(channelId -> channelId != null && channelId != 0L)
                .count();
        int threadCount = destination.threads == null ? 0 : (int) destination.threads.stream()
                .filter(thread -> thread.channelId != null && thread.channelId != 0L)
                .count();
        return channelCount + threadCount;
    }
}
