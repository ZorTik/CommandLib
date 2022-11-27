package me.zort.commandlib;

import java.util.List;

public interface UsagePrinter<S> {

    /**
     * Prints formatted usages to the user.
     *
     * @param sender The sender to send the usages to.
     * @param commandName The command name that the player used.
     * @param args Command arguments that he tried to use.
     * @param usages The usages for entries.
     */
    void print(S sender, String commandName, String[] args, List<String> usages);

}
