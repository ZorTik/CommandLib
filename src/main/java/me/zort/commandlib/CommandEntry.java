package me.zort.commandlib;

import com.google.common.primitives.Primitives;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.zort.commandlib.annotation.Arg;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandRegistration;
import me.zort.commandlib.annotation.Suggest;
import me.zort.commandlib.suggestion.SuggestionProvider;
import me.zort.commandlib.suggestion.SuggestionProviderStore;
import me.zort.commandlib.util.Arrays;
import me.zort.commandlib.util.NamingStrategy;
import me.zort.commandlib.util.PrimitiveParser;
import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

import static me.zort.commandlib.util.CommandUtil.parseCommandName;

public class CommandEntry {
    private final CommandLib<?> lib;
    @Getter
    private final CommandEntryMeta meta;
    @Getter
    private final Object mappingObject;
    private final Method method;
    private final Command annot;

    @AllArgsConstructor
    @Data
    private static class ParseResult {
        private final Map<String, String> placeholders;
        private final String[] relativeArgs;
    }

    @AllArgsConstructor
    @Getter
    public static class ParsingProcessData {
        private final Map<String, String> placeholders;
        private final String[] relativeArgs;
        private final String commandName;
        private final String[] args;
        private final Set<Class<? extends CommandArgumentRule>> passedRules;
    }

    public CommandEntry(CommandLib<?> lib, Object mappingObject, Method method) {
        this.lib = lib;
        this.mappingObject = mappingObject;
        this.method = method;
        if(!method.isAnnotationPresent(Command.class)) {
            throw new IllegalArgumentException("Method is not command-like!");
        }
        this.annot = method.getDeclaredAnnotation(Command.class);
        this.meta = buildMeta();
    }

    public boolean invoke(Object sender, String commandName, String[] args) {
        ParseResult parseResult = parse(commandName, args);
        if(parseResult == null) {
            // Is not passing conditions.
            return false;
        }
        if(!meta.getRequiredSenderType().isAssignableFrom(sender.getClass())) {
            // Invalid sender type.
            String[] invalidSenderMessage = meta.getInvalidSenderMessage();
            if(invalidSenderMessage.length > 0) {
                lib.sendMessage(sender, Arrays.map(invalidSenderMessage, lib::colorize));
            }
            // Returning true because we don't want to invoke invalid syntax methods.
            return true;
        }

        // Build arguments for method invocation
        Map<String, String> placeholders = parseResult.getPlaceholders();
        String[] relativeArgs = parseResult.getRelativeArgs();
        Parameter[] params = method.getParameters();
        Object[] invokeArgs = new Object[params.length];
        for(int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Object value = null;
            if(param.isAnnotationPresent(Arg.class)) {
                // Parameter uses name specific declaration
                // {agumentName}
                // void method(@Arg("argumentName") String argument)
                Arg arg = param.getAnnotation(Arg.class);
                String paramName = arg.value();
                value = placeholders.get(paramName);

                if(value != null) {
                    PrimitiveParser parser = new PrimitiveParser((String) value, param.getType());
                    if(parser.isParsed()) {
                        value = parser.getAsObject();
                    } else {
                        lib.getUsagePrinterManager().invokeLoggerFor(sender, commandName, args, false);
                    }
                    log("Param " + paramName + " is " + value.getClass().getSimpleName() + ": " + value);
                }
            } else if(param.getType().equals(String[].class)) {
                value = relativeArgs;
                log("Param " + param.getName() + " is String[]: " + value);
            } else if(param.getType().isAssignableFrom(sender.getClass())) {
                value = sender;
                log("Param " + param.getName() + " is sender: " + value);
            } else {
                log("Unsupported parameter type: " + param.getType().getName());
            }
            invokeArgs[i] = value;
        }
        try {
            lib.log("Placeholders after parse: " + CommandLib.GSON.toJson(placeholders));
            if(Primitives.wrap(method.getReturnType()).equals(Boolean.class)) {
                return (Boolean) method.invoke(mappingObject, invokeArgs);
            } else {
                lib.log("Invoking command " + commandName + " with args " + java.util.Arrays.toString(invokeArgs));
                method.invoke(mappingObject, invokeArgs);
            }
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean passes(String commandName, String[] args) {
        String syntax = getSyntax();
        String[] syntaxArgs = getSyntaxArgs();
        if(!matchesName(commandName) || !passesArgs(args) || (!syntax.endsWith("{...args}") && syntaxArgs.length != args.length)) {
            // Provided is not this command.
            return false;
        }
        return true;
    }

    public boolean passesArgs(String[] args) {
        String[] syntaxArgs = getSyntaxArgs();

        if(args.length > syntaxArgs.length && !hasRelativeArgs()) {
            // Provided args are longer than syntax args and syntax does not
            // have relative args.
            return false;
        }

        for(int i = 0; i < syntaxArgs.length; i++) {
            String syntaxArg = syntaxArgs[i];
            if(syntaxArg.startsWith("{") && syntaxArg.endsWith("}")) {
                // This is a placeholder.

                if(syntaxArg.contains("...args")) {
                    return true;
                }
                continue;
            }

            if(i >= args.length) {
                // We're out of args.
                return false;
            }
            if(!syntaxArg.equals(args[i]) && !isPlaceholderArg(syntaxArg)) {
                // This is not the same argument.
                return false;
            }
        }
        return true;
    }

    public boolean matchesForSuggestion(String commandName, String[] args) {
        String[] syntaxArgs = getSyntaxArgs();
        if(!matchesName(commandName)
                || syntaxArgs.length == 0
                || (args.length > syntaxArgs.length && !syntaxArgs[syntaxArgs.length - 1].equals("...args"))) {
            return false;
        }
        for(int i = 0; i < args.length; i++) {
            String arg = args[i];
            if(i >= syntaxArgs.length && syntaxArgs[syntaxArgs.length - 1].equals("...args")) {
                // We're in the last argument, and it's a varargs.
                return true;
            } else if(i >= syntaxArgs.length) {
                return false;
            } else if(isPlaceholderArg(syntaxArgs[i])) {
                continue;
            }

            if (!syntaxArgs[i].startsWith(arg)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getSuggestions(Object sender, String commandName, String[] args) {
        //args = (String[]) ArrayUtils.add(args, "");
        if(matchesForSuggestion(commandName, args)) {
            int argIndex = args.length - 1;
            String[] mappingArgs = annot.value().split(" ");
            try {
                if(mappingArgs[0].startsWith("/"))
                    mappingArgs = (String[]) ArrayUtils.subarray(mappingArgs, 1, mappingArgs.length);
                String arg = mappingArgs[argIndex];
                if(!arg.equals("{...args}")) {
                    if (isPlaceholderArg(arg)) {
                        // This argument is a placeholder.
                        String name = arg.substring(1, arg.length() - 1);
                        Parameter parameter = getArgParameter(name);
                        if (parameter != null && parameter.isAnnotationPresent(Suggest.class)) {
                            SuggestionProvider provider = lib.getSuggestionStore()
                                    .getProvider(parameter.getDeclaredAnnotation(Suggest.class).value());
                            if (provider != null) {
                                return provider.suggest(sender, args[args.length - 1]);
                            }
                        }
                    } else {
                        return Collections.singletonList(arg);
                    }
                }
            } catch(IndexOutOfBoundsException ignored) {}
        }
        return Collections.emptyList();
    }

    public String buildUsage() {
        String[] syntaxArgs = getSyntaxArgs();
        String[] usageArgs = new String[syntaxArgs.length];

        for(int i = 0; i < syntaxArgs.length; i++) {
            usageArgs[i] = prepareUsageArg(syntaxArgs[i]);
        }

        return usageArgs.length > 0
                ? "/" + getName() + " " + String.join(" ", usageArgs)
                : "/" + getName();
    }

    private String prepareUsageArg(String arg) {
        if(isPlaceholderArg(arg)) {
            String name = arg.substring(1, arg.length() - 1);
            arg = "(" + NamingStrategy.javaToHumanConst(name) + ")";
        }
        return arg;
    }

    public boolean isEligibleForUsage() {
        return !isErrorHandler() && !isMiddleware();
    }

    public static boolean isPlaceholderArg(String arg) {
        return arg.startsWith("{") && arg.endsWith("}");
    }

    private ParseResult parse(String commandName, String[] args) {
        return parse(commandName, args, true);
    }

    private ParseResult parse(String commandName, String[] args, boolean debug) {
        String syntax = getSyntax();
        String[] syntaxArgs = getSyntaxArgs();
        if(!passes(commandName, args)) {
            return null;
        }
        if(syntax.endsWith(" {...args}")) {
            syntaxArgs = (String[]) ArrayUtils.remove(syntaxArgs, syntaxArgs.length - 1);
        } else if(syntax.equals("{...args}")) {
            syntaxArgs = new String[0];
        } else if(syntax.isEmpty()) {
            syntaxArgs = new String[0];
        }
        if(args.length - syntaxArgs.length < 0) {
            // Not enough arguments.
            return null;
        }

        Map<String, String> ph = new HashMap<>();
        String[] ra = new String[args.length - syntaxArgs.length];
        for(int i = 0; i < args.length; i++) {
            boolean isRelativeArg = i >= syntaxArgs.length;
            if(isRelativeArg) {
                // Saving as relative argument.
                ra[i - syntaxArgs.length] = args[i];
            } else {
                Set<Class<? extends CommandArgumentRule>> passedRules = new HashSet<>();
                ParsingProcessData processData = new ParsingProcessData(ph, ra, commandName, args, passedRules);
                String syntaxName = syntaxArgs[i];

                List<CommandArgumentRule> rules = lib.getArgumentRules().getAllInContext("/" + parseCommandName(commandName) + " " + String.join(" ", args));

                for (CommandArgumentRule rule : new ArrayList<>(rules)) {
                    boolean passed = false;

                    if (rule.test(args[i], syntaxName, processData)) {
                        passedRules.add(rule.getClass());
                        passed = true;
                    }

                    if(!passed)
                        log("Argument " + args[i] + " failed rule " + rule.getClass().getSimpleName());
                }

                if(!rules.isEmpty() && passedRules.isEmpty()) {
                    // Command does not match this syntax.
                    return null;
                }
            }
        }
        if(debug) {
            log("Parsed command: " + commandName + " with args: " + java.util.Arrays.toString(args));
            log("Syntax args: " + java.util.Arrays.toString(syntaxArgs));
            log("ph: " + CommandLib.GSON.toJson(ph));
            log("ra: " + CommandLib.GSON.toJson(ra));
        }
        return new ParseResult(ph, ra);
    }

    public void register() {
        lib.register(this);
    }

    public String getName() {
        return getMeta().getName().replaceAll("/", "");
    }

    public String getSyntax() {
        String syntax = annot.value();
        if(isErrorHandler() && !syntax.endsWith(" {...args}") && !syntax.equals("{...args}")) {
            syntax += " {...args}";
        }
        return syntax;
    }

    public String[] getSyntaxArgs() {
        String syntax = getSyntax();
        if (syntax.isEmpty())
            return new String[0];

        return syntax.split(" ");
    }

    public Parameter getArgParameter(String name) {
        for(Parameter parameter : method.getParameters()) {
            if(parameter.isAnnotationPresent(Arg.class)) {
                Arg arg = parameter.getDeclaredAnnotation(Arg.class);
                if(arg.value().equals(name)) {
                    return parameter;
                }
            }
        }
        return null;
    }

    public boolean isErrorHandler() {
        return annot.unknown();
    }

    public boolean isMiddleware() {
        return Primitives.wrap(method.getReturnType()).equals(Boolean.class);
    }

    public boolean hasRelativeArgs() {
        String[] syntaxArgs = getSyntaxArgs();
        return syntaxArgs.length > 0 && syntaxArgs[syntaxArgs.length - 1].equals("{...args}");
    }

    public boolean matchesName(String name) {
        return getName().equalsIgnoreCase(name.replaceAll("/", ""));
    }

    private void log(String s) {
        lib.log(s);
    }

    private CommandEntryMeta buildMeta() {
        CommandEntryMeta meta = new CommandEntryMeta();
        if(!method.getDeclaringClass().isAnnotationPresent(CommandRegistration.class)) {
            throw new RuntimeException(String.format("Command class [%s] is not annotated with @CommandRegistration!",
                    mappingObject.getClass().getName()));
        }

        CommandRegistration commandRegistration = method.getDeclaringClass().getDeclaredAnnotation(CommandRegistration.class);
        meta.setName(commandRegistration.name());
        meta.setDescription(commandRegistration.description());
        meta.setUsage(commandRegistration.usage());
        meta.setRequiredSenderType(commandRegistration.requiredSenderType());
        meta.setInvalidSenderMessage(commandRegistration.invalidSenderMessage());
        if(meta.getRequiredSenderType().equals(Object.class)) {
            // Setting required sender type to sender type in the method
            // if is present. If meta has specified sender type other
            // than object, we'll use that.
            for(Parameter parameter : method.getParameters()) {
                if(lib.getDefaultSenderType().isAssignableFrom(parameter.getType())) {
                    // We'll use this sender type as required.
                    meta.setRequiredSenderType(parameter.getType());
                    break;
                }
            }
        }
        return meta;
    }

}
