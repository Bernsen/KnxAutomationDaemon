package de.root1.kad.logicplugin.cron;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables scheduling for the method annotated.
 * <p/>
 * Cron format: minute, hour, day, month, day of the week
 */
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scheduled {
    String cron() default "";
}