package me.zort.commandlib;

import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;

@UtilityClass
public final class CommandUtil {

    public static boolean matchesArgs(String command, String argsSyntax) {
        command = parseArgs(command);
        argsSyntax = parseArgs(argsSyntax);

        String[] cmdArgs = command.split(" ");
        String[] syntaxArgs = argsSyntax.split(" ");

        if(cmdArgs.length > syntaxArgs.length && (syntaxArgs.length > 0 && !syntaxArgs[syntaxArgs.length - 1].equals("{...args}"))) {
            return false;
        } else if(cmdArgs.length < syntaxArgs.length) {
            return false;
        }

        for(int i = 0; i < cmdArgs.length; i++) {
            String arg = cmdArgs[i];

            if(i >= syntaxArgs.length) {
                return true;
            } else if(CommandEntry.isPlaceholderArg(syntaxArgs[i])) {
                continue;
            } else if(!arg.equals(syntaxArgs[i])) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    public static String parseCommandName(String syntax) {
        return parseCommandName(syntax.split(" "));
    }

    @Nullable
    public static String parseCommandName(String[] syntax) {
        if(syntax.length == 0) return null;
        String commandName = syntax[0];
        if(commandName.startsWith("/"))
            commandName = commandName.substring(1);
        return commandName;
    }

    public static String parseArgs(String syntax) {
        if(syntax.startsWith("/")) {
            return syntax.replaceFirst("/" + parseCommandName(syntax), "");
        }
        return syntax;
    }

}
