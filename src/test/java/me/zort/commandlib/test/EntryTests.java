package me.zort.commandlib.test;

import me.zort.commandlib.test.entry.TestCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntryTests {

    private static final TestCommandLib testLibrary = new TestCommandLib(Collections.singletonList(new TestCommand()));

    @BeforeAll
    public static void setup() {
        testLibrary.setDebug(true);
        testLibrary.registerAll();
    }

    @Test
    public void testSingleInvoke() {
        TestCommandLib staticTestLibrary = testLibrary;
        UUID uuid = testLibrary.test("/test", new String[]{"command1"});

        assertEquals(2, testLibrary.getReport(uuid).countPassed());

        int commandsPassed = testLibrary.getReport(uuid).countPassed(entry -> !entry.isMiddleware() && !entry.isErrorHandler());

        assertEquals(1, commandsPassed);
    }

    @Test
    public void testMiddleware() {
        // Invoke it
        UUID uuid = testLibrary.test("/test", new String[]{"dontcontinue", "one"});

        assertTrue(testLibrary.getReport(uuid).hasPassed("/test {...args}"));
        assertFalse(testLibrary.getReport(uuid).hasPassed("/test dontcontinue one"));
    }

}