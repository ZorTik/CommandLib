package me.zort.commandlib.internal;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class CommandEntryMeta {

    private String description = "";
    private String usage = "";

}
