package com.discordsrv.bukkit;

import org.bukkit.Server;

import java.util.Set;

public class PaperCommandMap {

    public static final boolean IS_AVAILABLE;

    static {
        boolean is = false;
        try {
            Class<?> serverClass = Server.class;
            serverClass.getDeclaredMethod("getCommandMap");
            is = true;
        } catch (Throwable ignored) {}
        IS_AVAILABLE = is;
    }

    public static Set<String> getKnownCommands(Server server) {
        return server.getCommandMap().getKnownCommands().keySet();
    }
}
