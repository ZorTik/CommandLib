package me.zort.commandlib.rule;

import me.zort.commandlib.CommandArgumentRule;
import me.zort.commandlib.CommandEntry;

import java.util.Map;

public class PlaceholderArgumentRule implements CommandArgumentRule {
    @Override
    public boolean test(String arg, String syntaxArg, CommandEntry.ParsingProcessData data) {
        if(syntaxArg.startsWith("{") && syntaxArg.endsWith("}")) {
            Map<String, String> ph = data.getPlaceholders();
            ph.put(syntaxArg.substring(1, syntaxArg.length() - 1), arg);
            return true;
        }
        return false;
    }
}
