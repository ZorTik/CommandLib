package me.zort.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class CommandLibBukkit extends CommandLib {

    private final List<Command> registeredCommands;
    private final Plugin plugin;

    protected CommandLibBukkit(Object plugin, Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.plugin = (Plugin) plugin;
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
            CommandEntryMeta meta = entry.getMeta();
            Command command = new Command(entry.getName(), meta.getDescription(), meta.getUsage(), new ArrayList<>()) {
                @Override
                public boolean execute(CommandSender commandSender, String label, String[] args) {
                    invoke(commandSender, "/" + entry.getName(), args);
                    return true;
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    Object mappingObject = entry.getMappingObject();
                    List<String> tabCompletions = completeSubcommands(entry.getName(), args);
                    if (!tabCompletions.isEmpty()) {
                        return tabCompletions;
                    } else if (mappingObject instanceof TabCompleter
                            && (tabCompletions = ((TabCompleter) mappingObject).onTabComplete(sender, this, alias, args)) != null) {
                        return tabCompletions;
                    } else {
                        return super.tabComplete(sender, alias, args);
                    }
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

    private List<String> completeSubcommands(String commandName, String[] args) {
        return getCommands()
                .stream()
                .filter(entry -> entry.matchesName(commandName))
                .flatMap(entry -> entry.getSuggestions(commandName, args).stream())
                .collect(Collectors.toList());
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
        if(sender instanceof CommandSender) {
            ((CommandSender) sender).sendMessage(message);
        }
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
