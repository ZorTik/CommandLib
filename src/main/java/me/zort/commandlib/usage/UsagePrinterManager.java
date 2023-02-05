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

        Usage usageAnnot = usageAnnots.get(parsedCommandName);

        boolean matchesUsageArgs = CommandUtil.matchesArgs(String.join(" ", args), usageAnnot.invokeArgs());

        if(matchesUsageArgs)
            nonExistent = false;

        ArrayList<CommandEntry> temporaryEntries = new ArrayList<>(entryStorage);
        temporaryEntries.sort(Comparator.comparingInt(e -> e.getSyntaxArgs().length));

        int printedUsages = 0;

        if(usageAnnot.enableContextualHelp() && args.length > 0 && args[args.length - 1].equalsIgnoreCase("help")) {
            args = Arrays.copyOf(args, args.length - 1);

            List<String> usages = new ArrayList<>();
            for (CommandEntry e : temporaryEntries) {
                if (!e.matchesName(atomicCommandName.get()) || !e.matchesForSuggestion(commandName, args))
                    continue;

                String usage = e.buildUsage();
                if (usage.split(" ").length > args.length + 2) {
                    String[] spl = usage.split(" ");
                    usage = String.join(" ", Arrays.copyOfRange(spl, 0, args.length + 2)) + " help";
                }
                usages.add(usage);
            }

            usagePrinter.print(sender, parsedCommandName, args, usages);
            printedUsages = usages.size();
        } else if(nonExistent == canInvokeNonExistent(parsedCommandName)
        && (canInvokeNonExistent(parsedCommandName) || matchesUsageArgs)) {
            List<String> usages = new ArrayList<>();

            for (CommandEntry e : temporaryEntries) {
                if (!e.matchesName(atomicCommandName.get()) || !e.isEligibleForUsage())
                    continue;

                usages.add(e.buildUsage());
            }

            usagePrinter.print(sender, parsedCommandName, args, usages);
            printedUsages = usages.size();
        }

        return printedUsages > 0;
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
