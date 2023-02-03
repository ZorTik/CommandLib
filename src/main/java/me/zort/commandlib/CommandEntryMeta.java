package me.zort.commandlib;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CommandEntryMeta {

    private String name;
    private String description = "";
    private String usage = "";
    private Class<?> requiredSenderType = Object.class;
    private String[] invalidSenderMessage = {};

}
