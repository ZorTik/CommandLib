package me.zort.commandlib;

import me.zort.commandlib.annotation.Command;

public interface CommandArgumentRule {

    /**
     * Checks if the current argument in command entry can be used with
     * corresponding syntax argument specified in {@link Command} value
     * on same index.
     *
     * @param arg The argument inserted by the sender.
     * @param syntaxArg The syntax argument.
     * @param data Storage of current parsing process.
     * @return If the argument can be applied to syntax arg.
     */
    boolean test(String arg, String syntaxArg, CommandEntry.ParsingProcessData data);

}
