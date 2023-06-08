package com.discordsrv.bukkit;

import org.bukkit.Server;

import java.util.Set;

public class PaperCmdMap {

    public static Set<String> getMap(Server server) {
        return server.getCommandMap().getKnownCommands().keySet();
    }
}
