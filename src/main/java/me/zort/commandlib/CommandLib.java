package me.zort.commandlib;

import com.google.gson.Gson;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.Usage;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public abstract class CommandLib {

    public static final Gson GSON = new Gson();

    private final Iterable<Object> mappingObjects;
    @Getter(AccessLevel.PROTECTED)
    private final List<CommandEntry> commands;
    private final UsagePrinterManager usagePrinterManager;

    @Setter
    private boolean debug = false;

    protected CommandLib(Iterable<Object> mappingObjects) {
        this.mappingObjects = mappingObjects;
        this.commands = Collections.synchronizedList(new ArrayList<>());
        this.usagePrinterManager = new UsagePrinterManager(commands);
    }

    // Implementations should implement that commands
    // are not registered repeatedly.
    // This comes from behaviour of this library.
    public abstract void register(CommandEntry entry);
    public abstract void unregister(CommandEntry entry);
    public abstract void sendMessage(Object sender, String... message);
    public abstract String colorize(String message);
    public abstract Class<?> getDefaultSenderType();

    public void registerAll() {
        unregisterAll();
        mappingObjects.forEach(this::loadMappingObject);
        commands.forEach(this::register);
    }

    public void unregisterAll() {
        commands.forEach(this::unregister);
        commands.clear();
    }

    // Command name with slash.
    @SuppressWarnings("unchecked")
    protected void invoke(Object sender, String commandName, String[] args) {
        if(!commandName.startsWith("/")) {
            commandName = "/" + commandName;
        }

        boolean nonExistent = false;

        if(!doInvokeIf(sender, commandName, args, e -> !e.isErrorHandler())) {
            doInvokeIf(sender, commandName, args, CommandEntry::isErrorHandler);

            nonExistent = true;
        }

        usagePrinterManager.invokeLoggerFor(sender, commandName, args, nonExistent);
    }

    private boolean doInvokeIf(Object sender, String commandName, String[] args, Predicate<CommandEntry> pred) {
        ArrayList<CommandEntry> commands = new ArrayList<>(this.commands);
        commands.sort(Comparator.comparingInt(e -> e.getSyntax().split(" ").length));
        ArrayList<CommandEntry> iterCommands = new ArrayList<>(commands);
        boolean anySuccessful = false;
        for (CommandEntry entry : iterCommands) {
            if(!pred.test(entry)) continue;
            try {
                if(entry.invokeConditionally(sender, commandName, args)) {
                    anySuccessful = true;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        return anySuccessful;
    }

    public void log(String message) {
        if(debug) System.out.println(message);
    }

    private void loadMappingObject(Object obj) {
        Class<?> clazz = obj.getClass();
        for(Method method : clazz.getDeclaredMethods()) {
            if(method.isAnnotationPresent(Command.class)) {
                commands.add(new CommandEntry(this, obj, method));

                Command commandAnnot = method.getDeclaredAnnotation(Command.class);
                if(clazz.isAnnotationPresent(Usage.class) && !commandAnnot.unknown()) {
                    Usage usage = clazz.getDeclaredAnnotation(Usage.class);
                    String commandName = CommandUtil.parseCommandName(commandAnnot.value());

                    if(commandName != null) {
                        usagePrinterManager.registerUsageLogging(usage, commandName);
                    }

                }

            }
        }
    }

}
