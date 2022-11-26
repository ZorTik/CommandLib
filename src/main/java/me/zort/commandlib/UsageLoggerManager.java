package me.zort.commandlib;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import me.zort.commandlib.annotation.Usage;

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UsageLoggerManager {

    private final List<CommandEntry> entryStorage;
    private final Map<String, UsageLogger<?>> usageLoggers = Maps.newConcurrentMap();
    private final Map<String, Usage>  usageAnnots = Maps.newConcurrentMap();

    public void invokeLoggerFor(Object sender, String commandName, String[] args, boolean nonExistent) {
        AtomicReference<String> atomicCommandName = new AtomicReference<>(commandName);
        String parsedCommandName = CommandUtil.parseCommandName(new String[]{commandName});

        UsageLogger usageLogger = usageLoggers.get(parsedCommandName);
        if(usageLogger == null) return;

        if(nonExistent == canInvokeNonExistent(parsedCommandName)
        && (canInvokeNonExistent(parsedCommandName) || CommandUtil.matchesArgs(String.join(" ", args), usageAnnots.get(parsedCommandName).invokeArgs()))) {
            usageLogger.print(sender, parsedCommandName, args, entryStorage
                    .stream()
                    .filter(e -> e.matchesName(atomicCommandName.get()))
                    .map(CommandEntry::buildUsage)
                    .collect(Collectors.toList()));
        }
    }

    public void registerUsageLogging(Usage usage, String commandName) {
        Objects.requireNonNull(commandName, "Command name must not be null!");

        try {
            UsageLogger<?> usageLogger = usage.logger().getConstructor().newInstance();
            usageLoggers.put(commandName, usageLogger);
            usageAnnots.put(commandName, usage);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private boolean canInvokeNonExistent(String parsedCommandName) {
        return usageAnnots.get(parsedCommandName).invokeArgs().length() == 0;
    }

}
