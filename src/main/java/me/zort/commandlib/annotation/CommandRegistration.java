package me.zort.commandlib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// All new commands registered within this class
// will have this meta.
//
// @CommandMeta(description = "Kill command.")
// public class KillCommandHandlers
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandRegistration {

    String name();
    String description() default "";
    String usage() default "";
    @Deprecated
    Class<?> requiredSenderType() default Object.class;
    @Deprecated
    String[] invalidSenderMessage() default {};

}
