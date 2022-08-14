package me.zort.commandlib;

import me.zort.commandlib.internal.CommandEntry;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class CommandLibBukkit extends CommandLib {

    private final List<Command> registeredCommands;

    protected CommandLibBukkit(Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.registeredCommands = new ArrayList<>();
    }

    @Override
    public void register(CommandEntry entry) {
        boolean success = false;
        CommandMap commandMap = getCommandMap();
        if(commandMap != null) {
            if(commandMap.getCommand(entry.getName()) != null) {
                // Command is already registered.
                return;
            }
            Command command = new Command(entry.getName(), entry.getMeta().getDescription(), entry.getMeta().getUsage(), new ArrayList<>()) {
                @Override
                public boolean execute(CommandSender commandSender, String label, String[] args) {
                    invoke(commandSender, "/" + entry.getName(), args);
                    return true;
                }
            };
            if(commandMap.register(entry.getName(), command)) {
                registeredCommands.add(command);
                success = true;
            }
        }
        if(!success) {
            log("Failed to register command: " + entry.getName());
        }
    }

    @Override
    public void unregister(CommandEntry entry) {
        CommandMap commandMap = getCommandMap();
        if(commandMap != null) {
            registeredCommands
                    .stream()
                    .filter(c -> c.getName().equals(entry.getName()))
                    .findFirst().ifPresent(cmd -> {
                        cmd.unregister(commandMap);
                        registeredCommands.remove(cmd);
                    });
        }
    }

    private CommandMap getCommandMap() {
        try {
            final Field commandMapField = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(Bukkit.getServer());
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
