package com.discordsrv.common.command.game;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface GameCommandExecutionHelper {

    CompletableFuture<List<String>> suggestCommands(List<String> parts);
    List<String> getAliases(String command);
    boolean isSameCommand(String command1, String command2);

}
