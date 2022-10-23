package me.zort.commandlib;

import com.google.gson.Gson;
import lombok.Setter;
import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.internal.CommandEntry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public abstract class CommandLib {

    public static final Gson GSON = new Gson();

    private final Iterable<Object> mappingObjects;
    private final List<CommandEntry> commands;

    @Setter
    private boolean debug = false;

    protected CommandLib(Iterable<Object> mappingObjects) {
        this.mappingObjects = mappingObjects;
        this.commands = Collections.synchronizedList(new ArrayList<>());
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
    protected void invoke(Object sender, String commandName, String[] args) {
        if(!commandName.startsWith("/")) {
            commandName = "/" + commandName;
        }
        ArrayList<CommandEntry> commands = new ArrayList<>(this.commands);
        commands.sort(Comparator.comparingInt(e -> e.getSyntax().split(" ").length));
        ArrayList<CommandEntry> iterCommands = new ArrayList<>(this.commands);
        boolean anySuccessful = false;
        for(CommandEntry entry : iterCommands) {
            if(entry.isErrorHandler()) {
                continue;
            }
            try {
                if(entry.invokeConditionally(sender, commandName, args)) {
                    anySuccessful = true;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if(!anySuccessful) {
            for(CommandEntry command : iterCommands) {
                if(command.isErrorHandler()) {
                    try {
                        command.invokeConditionally(sender, commandName, args);
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void log(String message) {
        if(debug) {
            System.out.println(message);
        }
    }

    private void loadMappingObject(Object obj) {
        Class<?> clazz = obj.getClass();
        for(Method method : clazz.getDeclaredMethods()) {
            if(method.isAnnotationPresent(Command.class)) {
                commands.add(new CommandEntry(this, obj, method));
            }
        }
    }

}
