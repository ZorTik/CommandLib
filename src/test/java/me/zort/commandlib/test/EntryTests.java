package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;
import me.zort.commandlib.test.entry.TestCommand;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntryTests {

    private static final TestCommandLib testLibrary = new TestCommandLib(Collections.singletonList(new TestCommand()));

    @BeforeAll
    public static void setup() {
        testLibrary.setDebug(false);
        testLibrary.registerAll();
    }

    @Test
    public void testWithArgument() {
        UUID uuid = testLibrary.test("/test", new String[]{"command3", "argument"});
        assertEquals(2, testLibrary.getReport(uuid).countPassed());
        assertEquals(1, testLibrary.getReport(uuid).countPassedCommands());
    }

    @Test
    public void testSingleInvoke() {
        TestCommandLib staticTestLibrary = testLibrary;
        UUID uuid = testLibrary.test("/test", new String[]{"command1"});

        assertEquals(3, testLibrary.getReport(uuid).countPassed());

        int basicPassed = testLibrary.getReport(uuid).countPassedCommands();

        assertEquals(1, basicPassed);
    }

    @Test
    public void testWithoutArgs() {
        UUID uuid = testLibrary.test("/test", new String[0]);
        int basicPassed = testLibrary.getReport(uuid).countPassedCommands();
        assertEquals(1, basicPassed);
    }

    @Test
    public void testMiddleware() {
        // Invoke it
        UUID uuid = testLibrary.test("/test", new String[]{"dontcontinue", "one"});

        assertFalse(testLibrary.getReport(uuid).hasPassed("/test {...args}"));
        assertFalse(testLibrary.getReport(uuid).hasPassed("/test dontcontinue one"));
    }

    @Test
    public void testUnknown() {
        UUID uuid = testLibrary.test("/test", new String[]{"unknown"});
        assertEquals(1, testLibrary.getReport(uuid).countPassed(CommandEntry::isErrorHandler));
    }

    @Test
    public void testSuggestions() {
        Set<String> completions = testLibrary.completeSubcommands(null, "/test", new String[] {""});
        System.out.println(completions);
        assertEquals(4, completions.size());
        assertTrue(completions.contains("command1"));
        assertTrue(completions.contains("command2"));
        assertTrue(completions.contains("command3"));
        assertTrue(completions.contains("dontcontinue"));
    }

    @Test
    public void testSuggestionsByProvider() {
        Set<String> completions = testLibrary.completeSubcommands(null, "/test", new String[] {"command3", ""});
        System.out.println(completions);
        assertEquals(3, completions.size());
        assertTrue(completions.contains("suggestion1"));
        assertTrue(completions.contains("suggestion2"));
        assertTrue(completions.contains("suggestion3"));
    }

}
