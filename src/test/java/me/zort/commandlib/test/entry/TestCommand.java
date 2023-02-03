package me.zort.commandlib.test.entry;

import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandMeta;

import java.io.PrintStream;

@CommandMeta(
        usage = "/",
        description = ""
)
public class TestCommand {

    @Command("/test {...args}")
    public boolean shouldNeverInvokeMiddleware(String[] args) {
        System.out.println("shouldNeverInvokeMiddleware");
        return !args[0].equalsIgnoreCase("dontcontinue");
    }

    @Command("/test dontcontinue one")
    public void shouldNeverInvoke() {
        throw new IllegalStateException("This entry should never be invoked!");
    }

    @Command("/test command1")
    public boolean commandOneMiddleware() {
        System.out.println("commandOneMiddleware");
        // Just for clarifying that the middleware invoked.
        return true;
    }

    @Command("/test command1")
    public void commandOne(PrintStream printStream) {
        printStream.println("Command one invoked!");
    }

    @Command("/test command2")
    public void commandTwo(PrintStream printStream) {
        printStream.println("Command two invoked!");
    }

}
