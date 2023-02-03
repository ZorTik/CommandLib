package me.zort.commandlib;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import me.zort.commandlib.annotation.Usage;
import me.zort.commandlib.util.CommandUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class UsagePrinterManager {

    private final List<CommandEntry> entryStorage;
    private final Map<String, UsagePrinter<?>> usageLoggers = Maps.newConcurrentMap();
    private final Map<String, Usage>  usageAnnots = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked, rawtypes")
    public void invokeLoggerFor(Object sender, String commandName, String[] args, boolean nonExistent) {
        AtomicReference<String> atomicCommandName = new AtomicReference<>(commandName);
        String parsedCommandName = CommandUtil.parseCommandName(new String[]{commandName});

        UsagePrinter usagePrinter = usageLoggers.get(parsedCommandName);
        if(usagePrinter == null) return;

        boolean matchesUsageArgs = CommandUtil.matchesArgs(String.join(" ", args), usageAnnots.get(parsedCommandName).invokeArgs());

        if(matchesUsageArgs)
            nonExistent = false;

        if(nonExistent == canInvokeNonExistent(parsedCommandName)
        && (canInvokeNonExistent(parsedCommandName) || matchesUsageArgs)) {


            usagePrinter.print(sender, parsedCommandName, args, entryStorage
                    .stream()
                    .filter(e -> e.matchesName(atomicCommandName.get()))
                    .filter(CommandEntry::isEligibleForUsage)
                    .sorted(Comparator.comparingInt(e -> e.getSyntaxArgs().length))
                    .map(CommandEntry::buildUsage)
                    .collect(Collectors.toList()));
        }
    }

    public void registerUsageLogging(Usage usage, String commandName) {
        Objects.requireNonNull(commandName, "Command name must not be null!");

        try {
            UsagePrinter<?> usagePrinter = usage.printer().getConstructor().newInstance();
            usageLoggers.put(commandName, usagePrinter);
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
