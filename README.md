# CommandLib
Flexible library for Bukkit &amp; BungeeCord commands. Uses simple command registering and handling syntax with automatic command registration!

### Example:
```java
@CommandMeta(
    description = "Extended kill command!",
    invalidSenderMessage = "&cThis command can be used only as Player!"
)
public class KillCommandHandlers {

    @Command("/kill {...args}")
    public boolean permissionHandler(Player sender) {
        if(!sender.hasPermission("example.kill")) {
            sender.sendMessage("You don't have permission to do this!");
            return false;
        }
        return true;
    }

    @Command("/kill {nickname}")
    public void kill(Player sender, @Arg("nickname") String nickname) {
        System.out.println("Killing " + nickname);
    }

    @Command("/kill {nickname} {...args}")
    public void killWithReason(Player sender, @Arg("nickname") String nickname, String[] relativeArgs) {
        System.out.println("Killing " + nickname + " with reason: " + String.join(" ", relativeArgs));
    }
    
    @Command("/kill cmd1|cmd2")
    public boolean cmd1orcmd2Middleware(Player sender) {
        return sender.hasPermission("example.orcondition");
    }
    
    @Command("/kill cmd1")
    public void kill() {
        System.out.println("cmd1");
    }
    
    @Command("/kill cmd2")
    public void kill() {
        System.out.println("cmd2");
    }

    @Command(value = "/kill", unknown = true)
    public void killBadSyntax(Player sender) {
        System.out.println("Bad syntax!");
    }

}

// Enable
KillCommandHandlers handlers = new KillCommandHandlers();
CommandLib lib = new CommandLibBuilder(this)
        .withMapping(handlers)
        .register();

// Disable
lib.unregisterAll();

```
