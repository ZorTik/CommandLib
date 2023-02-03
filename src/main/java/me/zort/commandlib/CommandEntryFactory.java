package me.zort.commandlib;

import java.lang.reflect.Method;

public interface CommandEntryFactory {

    CommandEntry create(CommandLib commandLib, Object object, Method method);

}
