package me.zort.commandlib.test.sender;

public class CustomSenderImpl implements CustomSender {

    private final String name;

    public CustomSenderImpl(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
