package com.discordsrv.common.player.provider.model;

public class SkinInfo {

    private final String textureId;
    private final String model;

    public SkinInfo(String textureId, String model) {
        this.textureId = textureId;
        this.model = model;
    }

    public String textureId() {
        return textureId;
    }

    public String model() {
        return model;
    }
}
