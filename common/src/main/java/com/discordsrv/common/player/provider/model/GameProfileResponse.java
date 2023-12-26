package com.discordsrv.common.player.provider.model;

import java.util.List;

public class GameProfileResponse {

    public String id;
    public String name;
    public List<Property> properties;

    public static class Property {
        public String name;
        public String value;
    }
}
