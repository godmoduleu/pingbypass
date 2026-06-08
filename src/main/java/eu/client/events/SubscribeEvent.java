/*
*
* Orbit by Meteor Development
* https://github.com/MeteorDevelopment/orbit/blob/master/LICENSE
*
 */

package eu.client.events;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubscribeEvent {
    int priority() default 0;
}
