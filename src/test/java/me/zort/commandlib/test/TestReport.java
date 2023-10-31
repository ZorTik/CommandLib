package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class TestReport {

    private final Map<CommandEntry, Boolean> reports = new ConcurrentHashMap<>();

    public void addReport(CommandEntry entry, boolean passed) {
        reports.put(entry, passed);
    }

    public boolean hasPassed(String syntax) {
        for (CommandEntry entry : reports.keySet()) {

            if(entry.getSyntax().equals(syntax))
                return reports.get(entry);
        }
        return false;
    }

    public int countPassed() {
        return countPassed(entry -> true);
    }

    public int countPassed(Predicate<CommandEntry> predicate) {
        return (int) getPassed().stream().filter(predicate).count();
    }

    public int countPassedCommands() {
        return countPassed(e -> !e.isMiddleware() && !e.isErrorHandler());
    }

    public List<CommandEntry> getPassed() {
        List<CommandEntry> entries = new ArrayList<>();
        for (CommandEntry entry : this.reports.keySet()) {
            if(this.reports.get(entry))
                entries.add(entry);
        }
        return entries;
    }

}
