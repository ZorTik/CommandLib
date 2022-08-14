# CommandLib
Flexible library for Bukkit &amp; BungeeCord commands. Uses simple command registering and handling syntax with automatic command registration!
> This library can work either with Bukkit or BungeeCord!

### Example:
```
@CommandMeta(description = "Extended kill command!")
public class KillCommandHandlers {

    @Command("/kill {nickname}")
    public void kill(Player sender, String nickname) {
        System.out.println("Killing " + nickname);
    }

    @Command("/kill {nickname} {...args}")
    public void killWithReason(Player sender, String nickname, String[] relativeArgs) {
        System.out.println("Killing " + nickname + " with reason: " + String.join(" ", relativeArgs));
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
