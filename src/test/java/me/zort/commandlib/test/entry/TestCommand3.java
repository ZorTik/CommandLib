package me.zort.commandlib.test.entry;

import me.zort.commandlib.annotation.Command;
import me.zort.commandlib.annotation.CommandRegistration;
import me.zort.commandlib.test.sender.CustomSender;

@CommandRegistration(name = "/test")
public class TestCommand3 {

    @Command
    public void baseCommand(CustomSender sender) {
        System.out.println("Hello, world! Custom sender " + sender.getName() + "!");
    }

}
