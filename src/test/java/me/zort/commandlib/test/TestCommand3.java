package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;
import me.zort.commandlib.test.sender.CustomSender2;
import me.zort.commandlib.test.sender.CustomSenderImpl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestCommand3 {

    private static final TestCommandLib testLibrary = new TestCommandLib(Collections.singletonList(new me.zort.commandlib.test.entry.TestCommand3()));

    @BeforeAll
    public static void setup() {
        testLibrary.setDebug(false);
        testLibrary.registerAll();
    }

    @Test
    public void testWithCustomSender() {
        CustomSenderImpl sender = new CustomSenderImpl("Borek");
        UUID uuid = testLibrary.test(sender, "/test", new String[0]);
        assertEquals(1, testLibrary.getReport(uuid).countPassed(CommandEntry::isEligibleForUsage));
    }

    @Test
    public void testWithCustomSender2() {
        CustomSender2 sender = new CustomSender2();
        UUID uuid = testLibrary.test(sender, "/test", new String[0]);
        assertEquals(0, testLibrary.getReport(uuid).countPassed(CommandEntry::isEligibleForUsage));
    }

}
