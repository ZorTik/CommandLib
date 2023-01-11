package me.zort.commandlib.rule;

import me.zort.commandlib.CommandArgumentRule;
import me.zort.commandlib.CommandEntry;

public class GeneralArgumentRule implements CommandArgumentRule {
    @Override
    public boolean test(String arg, String syntaxArg, CommandEntry.ParsingProcessData data) {
        return syntaxArg.equals(arg);
    }
}
