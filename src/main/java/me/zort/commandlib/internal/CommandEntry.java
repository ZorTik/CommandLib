package me.zort.commandlib.internal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import me.zort.commandlib.CommandLib;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandMeta;
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
        }
    }

    public boolean invokeConditionally(Object sender, String commandName, String[] args) {
        ParseResult parseResult = parse(commandName, args);
        if(parseResult == null) {
            // Not passes conditions.
            return false;
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
            method.invoke(mappingObject, invokeArgs);
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
