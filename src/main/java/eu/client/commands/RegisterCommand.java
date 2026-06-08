package eu.client.commands;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface RegisterCommand {
    String name();
    String tag() default "";
    String description() default "No description.";
    String syntax() default "";
    String[] aliases() default {};
}
