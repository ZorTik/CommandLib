package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCommand2 {

    private static final TestCommandLib testLibrary = new TestCommandLib(Collections.singletonList(new me.zort.commandlib.test.entry.TestCommand2()));

    @BeforeAll
    public static void setup() {
        testLibrary.setDebug(false);
        testLibrary.registerAll();
    }

    @Test
    public void testUnknownWithoutArgument() {
        UUID uuid = testLibrary.test("/test", new String[0]);
        assertEquals(1, testLibrary.getReport(uuid).countPassed(CommandEntry::isErrorHandler));
    }

    @Test
    public void testUnknownWithArgument() {
        UUID uuid = testLibrary.test("/test", new String[]{"unknown"});
        assertEquals(1, testLibrary.getReport(uuid).countPassed(CommandEntry::isErrorHandler));
    }

}
