package me.zort.commandlib;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public final class CommandLibBukkit extends CommandLib<CommandSender> {

    private final List<Command> registeredCommands;
    private final Plugin plugin;
    private boolean anyRegistered = false;
    private RegisteringStrategy registeringStrategy = RegisteringStrategy.COMMAND_MAP;
    private LocalPreprocessListener preprocessListener = null;

    public enum RegisteringStrategy {
        COMMAND_MAP, PLUGIN, PREPROCESS
    }

    private interface LocalCommandExecutor extends CommandExecutor, TabCompleter {
    }

    private static class LocalPreprocessListener implements Listener {
        private final Map<String, BiConsumer<CommandSender, String[]>> execFuncs = new ConcurrentHashMap<>();
        private final Map<String, TabCompleter> tabCompleteFuncs = new ConcurrentHashMap<>();

        @EventHandler
        public void onCommandPreprocess(PlayerCommandPreprocessEvent e) {
            String[] args = e.getMessage().split(" ");
            BiConsumer<CommandSender, String[]> exec = execFuncs.get(args[0].substring(1));
            if (exec != null) {
                e.setCancelled(true);
                exec.accept(e.getPlayer(), Arrays.copyOfRange(args, 1, args.length));
            }
        }

        @EventHandler
        public void onTabComplete(TabCompleteEvent e) {
            // TODO
        }
    }

    CommandLibBukkit(Object plugin, Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.plugin = (Plugin) plugin;
        this.registeredCommands = new ArrayList<>();
    }

    public void useRegisteringStrategy(RegisteringStrategy registeringStrategy) {
        if (anyRegistered) {
            throw new IllegalStateException("Cannot change registering strategy after any command has been registered!");
        }
        this.registeringStrategy = registeringStrategy;
    }

    @Override
    public void register(CommandEntry entry) {
        anyRegistered = true;
        BiConsumer<CommandSender, String[]> exec = (sender, args) -> {
            invoke(sender, "/" + entry.getName(), args);
        };
        TabCompleter tabCompleteFunc = (sender, command, alias, args) -> {
            if (alias.endsWith(" ")) {
                // Input ends with empty space, so it is considered to be start of
                // new argument.
                args = Arrays.copyOf(args, args.length + 1);
                args[args.length - 1] = "";
            }

            Object mappingObject = entry.getMappingObject();
            List<String> tabCompletions = new ArrayList<>(completeSubcommands(sender, entry.getName(), args));
            if (!tabCompletions.isEmpty()) {
                return tabCompletions;
            } else if (mappingObject instanceof TabCompleter
                    && (tabCompletions = ((TabCompleter) mappingObject).onTabComplete(sender, command, alias, args)) != null) {
                return tabCompletions;
            } else {
                return null;
            }
        };

        boolean success = false;
        if (registeringStrategy.equals(RegisteringStrategy.COMMAND_MAP)) {
            success = registerUsingCommandMap(entry, exec, tabCompleteFunc);
        } else if (registeringStrategy.equals(RegisteringStrategy.PLUGIN)) {
            success = registerUsingPlugin(entry, exec, tabCompleteFunc);
        } else if (registeringStrategy.equals(RegisteringStrategy.PREPROCESS)) {
            success = registerUsingPreprocess(entry, exec, tabCompleteFunc);
        }
        if(!success) {
            log("Failed to register command: " + entry.getName());
        }
    }

    private boolean registerUsingCommandMap(
            CommandEntry entry,
            BiConsumer<CommandSender, String[]> exec,
            TabCompleter tabCompleteFunc
    ) {
        CommandMap commandMap = getCommandMap();
        if(commandMap != null) {
            if(registeredCommands
                    .stream()
                    .anyMatch(c -> c.getName().equalsIgnoreCase(entry.getName()))) {
                // Command is already registered.
                return false;
            }
            CommandEntryMeta meta = entry.getMeta();
            Command command = new Command(entry.getName(), meta.getDescription(), meta.getUsage(), new ArrayList<>()) {
                @Override
                public boolean execute(CommandSender commandSender, String label, String[] args) {
                    exec.accept(commandSender, args);
                    return true;
                }

                @Override
                public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
                    List<String> completions = tabCompleteFunc.onTabComplete(sender, this, alias, args);
                    if (completions == null) {
                        completions = super.tabComplete(sender, alias, args);
                    }
                    return completions;
                }
            };
            if(commandMap.register(plugin.getName(), command)) {
                registeredCommands.add(command);
                return true;
            }
        }
        return false;
    }

    private boolean registerUsingPlugin(
            CommandEntry entry,
            BiConsumer<CommandSender, String[]> exec,
            TabCompleter tabCompleteFunc
    ) {
        if (!(plugin instanceof JavaPlugin)) {
            log("Plugin is not instance of JavaPlugin!");
            return false;
        }
        PluginCommand command = ((JavaPlugin) plugin).getCommand(entry.getName());
        if (command == null) {
            log("Command '" + entry.getName() + "' does not exist in plugin.yml!");
            return false;
        }
        command.setExecutor(new LocalCommandExecutor() {
            @Override
            public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
                exec.accept(sender, args);
                return true;
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
                return tabCompleteFunc.onTabComplete(sender, command, label, args);
            }
        });
        return true;
    }

    private boolean registerUsingPreprocess(
            CommandEntry entry,
            BiConsumer<CommandSender, String[]> exec,
            TabCompleter tabCompleteFunc
    ) {
        if (preprocessListener == null) {
            preprocessListener = new LocalPreprocessListener();
            Bukkit.getPluginManager().registerEvents(preprocessListener, plugin);
        }
        preprocessListener.execFuncs.put(entry.getName(), exec);
        preprocessListener.tabCompleteFuncs.put(entry.getName(), tabCompleteFunc);
        return true;
    }

    @Override
    public void unregister(CommandEntry entry) {
        if (registeringStrategy.equals(RegisteringStrategy.COMMAND_MAP)) {
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
        } else if (registeringStrategy.equals(RegisteringStrategy.PLUGIN) && plugin instanceof JavaPlugin) {
            PluginCommand command = ((JavaPlugin) plugin).getCommand(entry.getName());
            if (command != null) {
                command.setExecutor(null);
            }
        } else if (registeringStrategy.equals(RegisteringStrategy.PREPROCESS)) {
            if (preprocessListener == null) {
                return;
            }
            preprocessListener.execFuncs.remove(entry.getName());
            preprocessListener.tabCompleteFuncs.remove(entry.getName());
            if (preprocessListener.execFuncs.isEmpty() && preprocessListener.tabCompleteFuncs.isEmpty()) {
                HandlerList.unregisterAll(preprocessListener);
                preprocessListener = null;
            }
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
