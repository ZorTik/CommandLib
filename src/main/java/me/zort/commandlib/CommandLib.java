package me.zort.commandlib;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandRegistration;
import me.zort.commandlib.annotation.Usage;
import me.zort.commandlib.rule.GeneralArgumentRule;
import me.zort.commandlib.rule.OrArgumentRule;
import me.zort.commandlib.rule.PlaceholderArgumentRule;
import me.zort.commandlib.suggestion.SuggestionProviderStore;
import me.zort.commandlib.usage.UsagePrinterManager;
import me.zort.commandlib.util.CommandUtil;
import me.zort.commandlib.util.ContextualCollection;

import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// S = CommandSender
public abstract class CommandLib<S> implements Iterable<CommandMeta> {
    public static final Gson GSON = new Gson();

    private final Iterable<Object> mappingObjects;
    @Getter(AccessLevel.PROTECTED)
    private final List<CommandEntry> commands;
    @Getter(AccessLevel.PROTECTED)
    private final UsagePrinterManager usagePrinterManager;
    @Getter(AccessLevel.PROTECTED)
    private final ContextualCollection<CommandArgumentRule> argumentRules;
    @Getter
    private final SuggestionProviderStore suggestionStore;
    @Setter
    private CommandEntryFactory entryFactory;

    @Setter
    private boolean debug = false;

    protected CommandLib(Iterable<Object> mappingObjects) {
        this.mappingObjects = mappingObjects;
        this.commands = Collections.synchronizedList(new ArrayList<>());
        this.usagePrinterManager = new UsagePrinterManager(commands);
        this.argumentRules = new ContextualCollection<>();
        this.suggestionStore = new SuggestionProviderStore();
        this.entryFactory = CommandEntry::new;

        registerArgumentRule(new GeneralArgumentRule());
        registerArgumentRule(new PlaceholderArgumentRule());
        registerArgumentRule(new OrArgumentRule());
    }

    // Implementations should implement that commands
    // are not registered repeatedly.
    // This comes from behaviour of this library.
    public abstract void register(CommandEntry entry);
    public abstract void unregister(CommandEntry entry);
    public abstract void sendMessage(Object sender, String... message);
    public abstract String colorize(String message);
    public abstract Class<?> getDefaultSenderType();

    public boolean registerArgumentRule(CommandArgumentRule rule) {
        return registerArgumentRule("/", rule); // All commands.
    }

    /**
     * Registers rule for accepting arguments based on context.
     * <p>
     * Example:
     * <pre>
     *     // This registers rule for any command starting with /command
     *     commandLib.registerArgumentRule("/command", argumentRule);
     * </pre>
     *
     * @param context Context of the rule.
     * @param rule Rule to be registered.
     * @return True if rule was registered, false otherwise.
     */
    public boolean registerArgumentRule(String context, CommandArgumentRule rule) {
        return argumentRules.save(context, rule);
    }

    public void registerAll() {
        unregisterAll();
        mappingObjects.forEach(this::loadMappingObject);
        commands.forEach(this::register);
    }

    public void unregisterAll() {
        commands.forEach(this::unregister);
        commands.clear();
    }

    public Map<String, CommandMeta> getDistinctCommands() {
        return commands.stream().collect(Collectors.toMap(CommandEntry::getName, CommandEntry::getMeta));
    }

    // Command name with slash.
    protected void invoke(Object sender, String commandName, String[] args) {
        if(!commandName.startsWith("/")) {
            commandName = "/" + commandName;
        }
        if (args.length == 1 && args[0].isEmpty())
            args = new String[0];

        boolean nonExistent = false;

        final String commandNameFinal = commandName;
        final String[] argsFinal = args;

        if (commands.stream().noneMatch(e -> e.isEligibleForUsage() && e.passes(commandNameFinal, argsFinal))) {
            nonExistent = true;
        }

        if(!nonExistent && !invoke(sender, commandName, args, e -> !e.isErrorHandler())) {
            if (args.length > 0 && args[args.length - 1].equalsIgnoreCase("help")) {
                if (usagePrinterManager.invokeLoggerFor(sender, commandName, args, true))
                    return;
            }
            nonExistent = true;
        }

        if (nonExistent) {
            invoke(sender, commandName, args, CommandEntry::isErrorHandler);
        }

        usagePrinterManager.invokeLoggerFor(sender, commandName, args, nonExistent);
    }

    private boolean invoke(Object sender, String commandName, String[] args, Predicate<CommandEntry> pred) {
        ArrayList<CommandEntry> commands = new ArrayList<>(this.commands);
        commands.sort(Comparator.comparingInt(e -> e.getSyntax().split(" ").length));
        ArrayList<CommandEntry> iterCommands = new ArrayList<>(commands);
        boolean anySuccessful = false;
        for (CommandEntry entry : iterCommands) {
            if(!pred.test(entry)) {
                continue;
            }
            try {
                if(entry.invoke(sender, commandName, args)) {
                    anySuccessful = true;
                } else if(entry.isMiddleware() && entry.passes(commandName, args)) {
                    return false;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return anySuccessful;
    }

    public Set<String> completeSubcommands(S sender, String commandName, String[] args) {
        return getCommands()
                .stream()
                .filter(entry -> entry.matchesName(commandName))
                .flatMap(entry -> entry.getSuggestions(sender, commandName, args).stream())
                .collect(Collectors.toSet());
    }

    public void log(String message) {
        if(debug) System.out.println(message);
    }

    private void loadMappingObject(Object obj) {
        Class<?> clazz = obj.getClass();

        if (!clazz.isAnnotationPresent(CommandRegistration.class)) {
            return;
        }

        for(Method method : clazz.getDeclaredMethods()) {
            if(method.isAnnotationPresent(Command.class)) {
                //commands.add(new CommandEntry(this, obj, method));
                commands.add(entryFactory.create(this, obj, method));

                CommandRegistration registration = clazz.getDeclaredAnnotation(CommandRegistration.class);
                Command commandAnnot = method.getDeclaredAnnotation(Command.class);
                if(clazz.isAnnotationPresent(Usage.class) && !commandAnnot.unknown()) {
                    Usage usage = clazz.getDeclaredAnnotation(Usage.class);
                    String commandName = CommandUtil.parseCommandName(registration.name());

                    if(commandName != null)
                        usagePrinterManager.registerUsageLogging(usage, commandName);

                }
            } else if (method.getParameterCount() == 1
                    && method.getParameters()[0].getType().equals(SuggestionProviderStore.class)) {
                // Invoke provider modifying method
                // void populateProviders(SuggestionProviderStore store) {
                //    store.registerProvider(name, provider);
                // }
                //
                // @Command
                // void command(@Arg("arg") @Suggest(name) String arg) {
                // }
                try {
                    method.invoke(obj, suggestionStore);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Iterator<CommandMeta> iterator() {
        return getDistinctCommands().values().iterator();
    }
}
