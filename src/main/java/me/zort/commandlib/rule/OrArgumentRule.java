package me.zort.commandlib.rule;

import me.zort.commandlib.CommandArgumentRule;
import me.zort.commandlib.CommandEntry;

public class OrArgumentRule implements CommandArgumentRule {
    @Override
    public boolean test(String arg, String syntaxArg, CommandEntry.ParsingProcessData data) {
        if(syntaxArg.contains("|")) {
            String[] sides = syntaxArg.split("\\|");
            for (String side : sides) {
                if(side.equals(arg)) {
                    return true;
                }
            }
        }
        return false;
    }
}
