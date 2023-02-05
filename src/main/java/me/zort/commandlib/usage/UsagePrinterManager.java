package me.zort.commandlib.usage;

import com.google.common.collect.Maps;
import lombok.RequiredArgsConstructor;
import me.zort.commandlib.CommandEntry;
import me.zort.commandlib.annotation.Usage;
import me.zort.commandlib.util.CommandUtil;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor
public class UsagePrinterManager {

    private final List<CommandEntry> entryStorage;
    private final Map<String, UsagePrinter<?>> usageLoggers = Maps.newConcurrentMap();
    private final Map<String, Usage> usageAnnots = Maps.newConcurrentMap();

    @SuppressWarnings("unchecked, rawtypes")
    public boolean invokeLoggerFor(Object sender, String commandName, String[] args, boolean nonExistent) {
        AtomicReference<String> atomicCommandName = new AtomicReference<>(commandName);
        String parsedCommandName = CommandUtil.parseCommandName(new String[]{commandName});

        UsagePrinter usagePrinter = usageLoggers.get(parsedCommandName);
        if(usagePrinter == null) return false;

        boolean matchesUsageArgs = CommandUtil.matchesArgs(String.join(" ", args), usageAnnots.get(parsedCommandName).invokeArgs());

        if(matchesUsageArgs)
            nonExistent = false;

        if(nonExistent == canInvokeNonExistent(parsedCommandName)
        && (canInvokeNonExistent(parsedCommandName) || matchesUsageArgs)) {

            ArrayList<CommandEntry> temporaryEntries = new ArrayList<>(entryStorage);
            temporaryEntries.sort(Comparator.comparingInt(e -> e.getSyntaxArgs().length));

            List<String> usages = new ArrayList<>();

            for (CommandEntry e : temporaryEntries) {
                if (!e.matchesName(atomicCommandName.get()) || !e.isEligibleForUsage())
                    continue;

                usages.add(e.buildUsage());
            }

            usagePrinter.print(sender, parsedCommandName, args, usages);

            return usages.size() > 0;
        }
        return false;
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
