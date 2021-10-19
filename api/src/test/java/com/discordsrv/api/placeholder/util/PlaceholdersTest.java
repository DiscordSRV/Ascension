package com.discordsrv.api.placeholder.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PlaceholdersTest {

    @Test
    public void orderTest() {
        Placeholders placeholders = new Placeholders("a");

        placeholders.replace("b", "c");
        placeholders.replace("a", "b");

        assertEquals("b", placeholders.toString());
    }
}
