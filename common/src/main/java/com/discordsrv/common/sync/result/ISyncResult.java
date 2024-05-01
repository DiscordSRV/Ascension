package com.discordsrv.common.sync.result;

import com.discordsrv.api.placeholder.util.Placeholders;

public interface ISyncResult {

    boolean isSuccess();
    String getFormat();

    default String format(String gameTerm, String discordTerm) {
        return new Placeholders(getFormat())
                .replace("%g", gameTerm)
                .replace("%d", discordTerm)
                .toString();
    }

}
