package me.zort.commandlib.test.entry;

import me.zort.commandlib.annotation.Arg;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandRegistration;
import me.zort.commandlib.annotation.Suggest;
import me.zort.commandlib.suggestion.SuggestionProviderStore;

import java.io.PrintStream;
import java.util.List;

@CommandRegistration(name = "/test")
public class TestCommand {

    @Command("{...args}")
    public boolean shouldNeverInvokeMiddleware(String[] args) {
        System.out.println("shouldNeverInvokeMiddleware");
        if (args.length > 0 && args[0].equalsIgnoreCase("dontcontinue"))
            return false;
        return true;
    }

    @Command
    public void withoutArgs() {
        System.out.println("Without args invoked!");
    }

    @Command("dontcontinue one")
    public void shouldNeverInvoke() {
        throw new IllegalStateException("This entry should never be invoked!");
    }

    @Command("command1")
    public boolean commandOneMiddleware() {
        System.out.println("commandOneMiddleware");
        // Just for clarifying that the middleware invoked.
        return true;
    }

    @Command("command1")
    public void commandOne(PrintStream printStream) {
        printStream.println("Command one invoked!");
    }

    @Command("command2")
    public void commandTwo(PrintStream printStream) {
        printStream.println("Command two invoked!");
    }

    @Command("command3 {arg}")
    public void commandThree(
            PrintStream printStream,
            @Arg("arg") @Suggest("argProvider") String arg
    ) {
        printStream.println("Command three invoked with: " + arg);
    }

    @Command(value = "{...args}", unknown = true)
    public void unknown(PrintStream printStream) {
        printStream.println("Unknown command!");
    }

    public void suggestions(SuggestionProviderStore store) {
        store.registerProvider("argProvider", (sender, arg) -> {
            return List.of("suggestion1", "suggestion2", "suggestion3");
        });
    }

}
