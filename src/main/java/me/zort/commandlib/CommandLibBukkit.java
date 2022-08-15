package me.zort.commandlib;

import me.zort.commandlib.internal.CommandEntry;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommandLibBukkit extends CommandLib {

    private final List<Command> registeredCommands;
    private final Plugin plugin;

    protected CommandLibBukkit(Plugin plugin, Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.plugin = plugin;
        this.registeredCommands = new ArrayList<>();
    }

    @Override
    public void register(CommandEntry entry) {
        boolean success = false;
        CommandMap commandMap = getCommandMap();
        if(commandMap != null) {
            if(registeredCommands
                    .stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(entry.getName()))) {
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
            if(commandMap.register(plugin.getName(), command)) {
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
                        unregister(cmd);
                        registeredCommands.remove(cmd);
                    });
        }
    }

    private void unregister(Command command) {
        CommandMap commandMap = getCommandMap();
        try {
            Field kcField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            kcField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, Command> kc = (Map<String, Command>) kcField.get(commandMap);
            String cmdName = command.getName().toLowerCase(Locale.ENGLISH).trim();
            String fallbackPrefix = plugin.getName().toLowerCase(Locale.ENGLISH).trim();
            // Future update: Implement aliases.
            kc.remove(cmdName);
            kc.remove(fallbackPrefix + ":" + cmdName);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void sendMessage(Object sender, String... message) {
        if(!(sender instanceof CommandSender)) {
            return;
        }
        ((CommandSender) sender).sendMessage(message);
    }

    @Override
    public String colorize(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    @Override
    public Class<?> getDefaultSenderType() {
        return CommandSender.class;
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
