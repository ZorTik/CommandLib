package me.zort.commandlib.suggestion;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SuggestionProviderStore {

    private final Map<String, SuggestionProvider> providers = new ConcurrentHashMap<>();

    public SuggestionProviderStore registerProvider(String name, SuggestionProvider provider) {
        providers.put(name, provider);
        return this;
    }

    public SuggestionProvider getProvider(String name) {
        return providers.get(name);
    }

}
