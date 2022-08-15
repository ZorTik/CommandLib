package me.zort.commandlib;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

public class CommandLibBuilder {

    private final boolean proxy;
    private final List<Object> mappingObjects;
    private final Object plugin;

    public CommandLibBuilder(Object plugin) {
        this.plugin = plugin;
        Class<?> checkClass = null;
        try {
            checkClass = Class.forName("org.bukkit.plugin.Plugin");
        } catch (ClassNotFoundException ignored) {}
        try {
            checkClass = Class.forName("net.md_5.bungee.api.plugin.Plugin");
        } catch(ClassNotFoundException ignored) {}

        checkArgument(checkClass != null, "Current platform is not supported!");
        checkArgument(checkClass.isAssignableFrom(plugin.getClass()), "Provided plugin object type is not supported!");

        this.proxy = checkClass.getName().contains("net.md_5");
        this.mappingObjects = new ArrayList<>();
    }

    public CommandLibBuilder withMapping(Object mappingObject) {
        mappingObjects.add(mappingObject);
        return this;
    }

    public CommandLib register() {
        CommandLib commandLib = proxy
                ? new CommandLibProxy(plugin, mappingObjects)
                : new CommandLibBukkit(plugin, mappingObjects);
        commandLib.registerAll();
        return commandLib;
    }

}
