package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;
import me.zort.commandlib.CommandLib;

import java.lang.reflect.Method;

public class TestCommandEntry extends CommandEntry {

    private final CommandLib commandLib;

    protected TestCommandEntry(CommandLib commandLib, Object mappingObject, Method method) {
        super(commandLib, mappingObject, method);
        this.commandLib = commandLib;
    }

    @Override
    public boolean invokeConditionally(Object sender, String commandName, String[] args) {
        boolean result = super.invokeConditionally(sender, commandName, args);
        if(commandLib instanceof TestCommandLib) {
            ((TestCommandLib) commandLib).report(this, result);
        }
        return result;
    }
}
