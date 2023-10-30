package me.zort.commandlib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Suggest annotation for command arguments.
 * <p></p>
 * Usage:
 * <pre>
 *  {@code @Command}
 *  public void command(@Suggest("providerName") String arg) {
 *      // ...
 *  }
 *
 *  public void suggestionProviders(SuggestionProviderStore store) {
 *      store.registerProvider("providerName", (sender, input) -> {
 *          // ...
 *      });
 *  }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Suggest {

    // SuggestionProvider name in the SuggestionProviderStore
    String value();

}
