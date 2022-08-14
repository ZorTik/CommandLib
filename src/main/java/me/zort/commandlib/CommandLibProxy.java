package me.zort.commandlib;

import me.zort.commandlib.internal.CommandEntry;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class CommandLibProxy extends CommandLib {

    private final PluginManager pluginManager;
    private final List<Command> registeredCommands;
    private final Plugin plugin;

    protected CommandLibProxy(Object plugin, Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.plugin = (Plugin) plugin;
        this.pluginManager = ProxyServer.getInstance().getPluginManager();
        this.registeredCommands = new ArrayList<>();
    }

    @Override
    public void register(CommandEntry entry) {
        if(pluginManager.getCommands()
                .stream()
                .anyMatch(e -> e.getValue().getName().equals(entry.getName()))) {
            // Command is already registered
            return;
        }
        Command command = new Command(entry.getName()) {
            @Override
            public void execute(CommandSender sender, String[] args) {
                invoke(sender, "/" + entry.getName(), args);
            }
        };
        pluginManager.registerCommand(plugin, command);
        registeredCommands.add(command);
    }

    @Override
    public void unregister(CommandEntry entry) {
        registeredCommands
                .stream()
                .filter(c -> c.getName().equals(entry.getName()))
                .findFirst().ifPresent(cmd -> {
                    pluginManager.unregisterCommand(cmd);
                    registeredCommands.remove(cmd);
                });
    }

}
