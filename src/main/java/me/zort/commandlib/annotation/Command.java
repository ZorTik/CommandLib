package me.zort.commandlib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {

    // @Command("player {player} {reason}")
    // public void kill(CommandSender sender, String player, String reason)

    // @Command("player {player} {reason} {...args}")
    // public void killWithRelativeArgs(CommandSender sender, String player, String reason, String[] relativeArgs)

    // Error/Bad Syntax handler
    // @Command(value = "player", unknown = true)
    // public void killBadSyntax(CommandSender sender)
    String value() default "";
    // If this mapping should work as unknown handler.
    boolean unknown() default false;

}
