package me.zort.commandlib.internal;

import com.google.common.primitives.Primitives;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.zort.commandlib.CommandLib;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandMeta;
import me.zort.commandlib.util.Arrays;
import org.apache.commons.lang.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.Map;

public class CommandEntry {

    @Getter
    private final CommandEntryMeta meta;

    private final CommandLib commandLib;
    private final Object mappingObject;
    private final Method method;
    private final Command annot;


    public CommandEntry(CommandLib commandLib, Object mappingObject, Method method) {
        this.commandLib = commandLib;
        this.mappingObject = mappingObject;
        this.method = method;
        if(!method.isAnnotationPresent(Command.class)) {
            throw new IllegalArgumentException("Method is not command-like!");
        }

        this.annot = method.getDeclaredAnnotation(Command.class);
        this.meta = new CommandEntryMeta();
        if(method.getDeclaringClass().isAnnotationPresent(CommandMeta.class)) {
            CommandMeta commandMeta = method.getDeclaringClass().getDeclaredAnnotation(CommandMeta.class);
            this.meta.setDescription(commandMeta.description());
            this.meta.setUsage(commandMeta.usage());
            this.meta.setRequiredSenderType(commandMeta.requiredSenderType());
            this.meta.setInvalidSenderMessage(commandMeta.invalidSenderMessage());
            if(meta.getRequiredSenderType().equals(Object.class)) {
                // Setting required sender type to sender type in the method
                // if is present. If meta has specified sender type other
                // than object, we'll use that.
                for(Parameter parameter : method.getParameters()) {
                    if(commandLib.getDefaultSenderType().isAssignableFrom(parameter.getType())) {
                        // We'll use this sender type as required.
                        this.meta.setRequiredSenderType(parameter.getType());
                        break;
                    }
                }
            }
        }
    }

    public boolean invokeConditionally(Object sender, String commandName, String[] args) {
        ParseResult parseResult = parse(commandName, args);
        if(parseResult == null) {
            // Is not passing conditions.
            return false;
        }
        if(!meta.getRequiredSenderType().isAssignableFrom(sender.getClass())) {
            // Invalid sender type.
            String[] invalidSenderMessage = meta.getInvalidSenderMessage();
            if(invalidSenderMessage.length > 0) {
                commandLib.sendMessage(sender, Arrays.map(invalidSenderMessage, commandLib::colorize));
            }
            // Returning true because we don't want to invoke invalid syntax methods.
            return true;
        }
        Map<String, String> placeholders = parseResult.getPlaceholders();
        String[] relativeArgs = parseResult.getRelativeArgs();

        Parameter[] params = method.getParameters();
        Object[] invokeArgs = new Object[params.length];
        for(int i = 0; i < params.length; i++) {
            Parameter param = params[i];
            Object value = null;
            if(param.getType().equals(String.class)) {
                value = placeholders.get(param.getName());
            } else if(param.getType().equals(String[].class)) {
                value = relativeArgs;
            } else if(param.getType().isAssignableFrom(sender.getClass())) {
                value = sender;
            } else {
                commandLib.log("Unsupported parameter type: " + param.getType().getName());
            }
            invokeArgs[i] = value;
        }
        try {
            if(Primitives.wrap(method.getReturnType()).equals(Boolean.class)) {
                // We're returning negated value, because boolean methods
                // work as middleware where positive result means continuation
                // of the process.
                return !(boolean) method.invoke(mappingObject, invokeArgs);
            } else {
                method.invoke(mappingObject, invokeArgs);
            }
            return true;
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return false;
        }
    }

    private ParseResult parse(String commandName, String[] args) {
        String syntax = getSyntax();
        String[] syntaxArgs = (String[]) ArrayUtils.subarray(syntax.split(" "), 1, syntax.split(" ").length);
        if(!matchesName(commandName) || (!syntax.endsWith(" {...args}") && syntaxArgs.length != args.length)) {
            // Provided is not this command.
            return null;
        }
        if(syntax.endsWith(" {...args}")) {
            syntaxArgs = (String[]) ArrayUtils.remove(syntaxArgs, syntaxArgs.length - 1);
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
                String syntaxName = syntaxArgs[i];
                if(syntaxName.startsWith("{") && syntaxName.endsWith("}")) {
                    // Saving as placeholder.
                    ph.put(syntaxName.substring(1, syntaxName.length() - 1), args[i]);
                } else if(!syntaxName.equals(args[i])) {
                    // Command does not match this syntax.
                    return null;
                }
            }
        }
        commandLib.log("Parsed command: " + commandName + " with args: " + java.util.Arrays.toString(args));
        commandLib.log("ph: " + CommandLib.GSON.toJson(ph));
        commandLib.log("ra: " + CommandLib.GSON.toJson(ra));
        return new ParseResult(ph, ra);
    }

    public void register() {
        commandLib.register(this);
    }

    public String getName() {
        String[] s = getSyntax().toLowerCase().split(" ");
        if(s[0].startsWith("/")) {
            return s[0].substring(1);
        } else {
            return s[0];
        }
    }

    public String getSyntax() {
        String syntax = annot.value();
        if(isErrorHandler() && !syntax.endsWith(" {...args}")) {
            syntax += " {...args}";
        }
        return syntax;
    }

    public boolean isErrorHandler() {
        return annot.unknown();
    }

    public boolean matchesName(String name) {
        return getName().equalsIgnoreCase(name.replaceAll("/", ""));
    }

    @AllArgsConstructor
    @Data
    private static class ParseResult {

        private final Map<String, String> placeholders;
        private final String[] relativeArgs;

    }

}
