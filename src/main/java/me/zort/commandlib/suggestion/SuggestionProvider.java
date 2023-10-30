package me.zort.commandlib.suggestion;

import java.util.List;

public interface SuggestionProvider {

    List<String> suggest(Object sender, String input);

}
