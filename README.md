# CommandLib
Flexible library for Bukkit &amp; BungeeCord commands. Uses simple command registering and handling syntax with automatic command registration!

### Features:
- Static commands
- Bukkit & BungeeCord support
- Built-in suggestion system
- Middlewares
- Error & Bad syntax handlers
- Conditional commands
- Custom sender support

### Example:
```java
@CommandRegistration(
    name = "/kill",
    description = "Extended kill command!",
    invalidSenderMessage = "&cThis command can be used only as Player!"
)
public class KillCommandHandlers {

    @Command("{...args}")
    public boolean permissionHandler(Player sender) {
        if(!sender.hasPermission("example.kill")) {
            sender.sendMessage("You don't have permission to do this!");
            return false;
        }
        return true;
    }

    @Command("{nickname}")
    public void kill(Player sender, @Arg("nickname") String nickname) {
        System.out.println("Killing " + nickname);
    }

    @Command("{nickname} {...args}")
    public void killWithReason(Player sender, @Arg("nickname") String nickname, String[] relativeArgs) {
        System.out.println("Killing " + nickname + " with reason: " + String.join(" ", relativeArgs));
    }
    
    @Command("cmd1|cmd2")
    public boolean cmd1orcmd2Middleware(Player sender) {
        return sender.hasPermission("example.orcondition");
    }
    
    @Command("cmd1")
    public void kill() {
        System.out.println("cmd1");
    }
    
    @Command("cmd2")
    public void kill() {
        System.out.println("cmd2");
    }

    @Command(value = "{...args}", unknown = true)
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
