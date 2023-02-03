package me.zort.commandlib.test;

import me.zort.commandlib.CommandEntry;
import me.zort.commandlib.CommandLib;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TestCommandLib extends CommandLib {

    private final List<CommandEntry> entries;
    private final Map<UUID, TestReport> reports;
    private UUID currentReportId = null;

    protected TestCommandLib(Iterable<Object> mappingObjects) {
        super(mappingObjects);
        this.entries = new CopyOnWriteArrayList<>();
        this.reports = new ConcurrentHashMap<>();
        setEntryFactory(TestCommandEntry::new);
    }

    protected UUID test(String commandName, String[] args) {
        UUID uuid = currentReportId = UUID.randomUUID();
        reports.put(uuid, new TestReport());

        super.invoke(System.out, commandName, args);

        return uuid;
    }

    protected void report(CommandEntry entry, boolean passed) {
        if(currentReportId == null) {
            throw new IllegalStateException("No report is currently active!");
        }
        reports.get(currentReportId).addReport(entry, passed);
    }

    protected TestReport getReport(UUID uuid) {
        return reports.get(uuid);
    }

    @Override
    public void register(CommandEntry entry) {
        entries.add(entry);
    }

    @Override
    public void unregister(CommandEntry entry) {
        entries.removeIf(e -> e.getName().equals(entry.getName()));
    }

    @Override
    public void sendMessage(Object sender, String... message) {
        System.out.println("[" + sender + "] " + String.join(" ", message));
    }

    @Override
    public String colorize(String message) {
        return message;
    }

    @Override
    public Class<?> getDefaultSenderType() {
        return PrintStream.class;
    }

    public List<CommandEntry> getEntries() {
        return entries;
    }
}
