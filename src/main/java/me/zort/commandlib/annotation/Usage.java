package me.zort.commandlib.annotation;

import me.zort.commandlib.UsagePrinter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Usage {

    /**
     * Args syntax to invoke usage on.
     * If invoked with args syntax present here,
     * usages are printed using {@link UsagePrinter}.
     * <p></p>
     * If this value is empty, usages are shown
     * on every bad invocation.
     *
     * @return The syntax.
     */
    String invokeArgs() default "";

    /**
     * Sets a printer to use for this mapping class.
     * @return The printer.
     */
    Class<? extends UsagePrinter> printer();

}