package com.discordsrv.fabric;

import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.common.util.ComponentUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.modcommon.AdventureCommandSourceStack;
import net.kyori.adventure.platform.modcommon.MinecraftServerAudiences;
import net.kyori.adventure.text.Component;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;

public class FabricAdventureUtil {

    //? if adventure: <6 {
    /*private final FabricServerAudiences adventure;
     *///?} else {
    private final MinecraftServerAudiences adventure;
    //?}

    public FabricAdventureUtil(FabricDiscordSRV discordSRV) {
        //? if adventure: <6 {
        /*this.adventure = FabricServerAudiences.of(discordSRV.getServer());
         *///?} else {
        this.adventure = MinecraftServerAudiences.of(discordSRV.getServer());
        //?}
    }

    //? if adventure: <6 {
    /*public FabricServerAudiences getAdventure() {
        return adventure;
    }
    *///?} else {
    public MinecraftServerAudiences getAdventure() {
        return adventure;
    }//?}

    public Component fromNative(Text text) {
        //? if adventure: <6 {
        // return adventure.toAdventure(text);
        //? } else {
        return adventure.asAdventure(text);
        //? }
    }

    public Component toAdventure(Text text) {
        return fromNative(text);
    }

    public Text toNative(Component component) {
        //? if adventure: <6 {
        // return adventure.toNative(component);
        //? } else {
        return adventure.asNative(component);
        //? }
    }

    public Text fromAdventure(Component component) {
        return toNative(component);
    }

    public MinecraftComponent toAPI(Component component) {
        return ComponentUtil.toAPI(component);
    }

    public MinecraftComponent toAPI(Text text) {
        return toAPI(fromNative(text));
    }

    public AdventureCommandSourceStack audience(@NotNull ServerCommandSource source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull ServerPlayerEntity source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull CommandOutput source) {
        return adventure.audience(source);
    }

    public @NotNull Audience audience(@NotNull Iterable<ServerPlayerEntity> players) {
        return adventure.audience(players);
    }
}
