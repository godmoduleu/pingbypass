/*
 *
 * Orbit by Meteor Development
 * https://github.com/MeteorDevelopment/orbit/blob/master/LICENSE
 *
 */

package eu.client.events;

import lombok.Getter;
import eu.client.EUClient;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Consumer;

@Getter
public class Listener {
    private static Method lookupMethod;

    private final Class<?> subscriber;
    private Consumer<Object> consumer;
    private final int priority;

    public Listener(Class<?> klass, Object object, Method method) {
        subscriber = method.getParameters()[0].getType();
        priority = method.getAnnotation(SubscribeEvent.class).priority();

        try {
            MethodType type = MethodType.methodType(void.class, method.getParameters()[0].getType());
            MethodHandle handle = MethodHandles.lookup().findVirtual(klass, method.getName(), type);
            MethodType invokedType = MethodType.methodType(Consumer.class, klass);

            consumer = (Consumer<Object>) LambdaMetafactory.metafactory(MethodHandles.lookup(), "accept", invokedType, MethodType.methodType(void.class, Object.class), handle, type).getTarget().invoke(object);
        } catch (Throwable throwable) {
            EUClient.LOGGER.error("The Event System threw an exception!", throwable);
        }
    }

    public void invoke(Object event) {
        consumer.accept(event);
    }
}
