package me.zort.commandlib.annotation;

import me.zort.commandlib.UsageLogger;

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
     * usages are printed using {@link UsageLogger}.
     * <p></p>
     * If this value is empty, usages are shown
     * on every bad invocation.
     *
     * @return The syntax.
     */
    String invokeArgs() default "";

    /**
     * Sets a logger to use for this mapping class.
     * @return The logger.
     */
    Class<? extends UsageLogger> logger();

}
