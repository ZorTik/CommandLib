package me.zort.commandlib.test.entry;

import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandRegistration;

import java.io.PrintStream;

@CommandRegistration(name = "/test")
public class TestCommand2 {

    @Command(value = "{...args}", unknown = true)
    public void unknown(PrintStream printStream) {
        printStream.println("Unknown command!");
    }

}
