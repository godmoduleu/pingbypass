/*
 *
 * Orbit by Meteor Development
 * https://github.com/MeteorDevelopment/orbit/blob/master/LICENSE
 *
 */

package eu.client.events;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public class EventHandler {
    private final Map<Object, List<Listener>> listeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Listener>> staticListeners = new ConcurrentHashMap<>();
    private final Map<Class<?>, List<Listener>> listenerMap = new ConcurrentHashMap<>();

    public void subscribe(Object object) {
        for (Listener listener : getListeners(object.getClass(), object)) {
            insert(listenerMap.computeIfAbsent(listener.getSubscriber(), aClass -> new CopyOnWriteArrayList<>()), listener);
        }
    }

    public void unsubscribe(Object object) {
        for (Listener listener : getListeners(object.getClass(), object)) {
            List<Listener> listeners = listenerMap.get(listener.getSubscriber());
            if (listeners == null) continue;

            listeners.remove(listener);
        }
    }

    public void post(Event event) {
        List<Listener> listeners = listenerMap.get(event.getClass());
        if (listeners == null) return;

        for (Listener listener : listeners) {
            listener.invoke(event);
        }
    }

    public boolean isListening(Class<?> eventKlass) {
        List<Listener> listeners = listenerMap.get(eventKlass);
        return listeners != null && !listeners.isEmpty();
    }

    private List<Listener> getListeners(Class<?> klass, Object object) {
        Function<Object, List<Listener>> func = o -> {
            List<Listener> listeners = new CopyOnWriteArrayList<>();
            processListeners(listeners, klass, object);

            return listeners;
        };

        if (object == null) return staticListeners.computeIfAbsent(klass, func);

        for (Object key : listeners.keySet()) {
            if (key != object) continue;
            return listeners.get(object);
        }

        List<Listener> appliedListeners = func.apply(object);
        listeners.put(object, appliedListeners);
        return appliedListeners;
    }

    private void processListeners(List<Listener> listeners, Class<?> klass, Object object) {
        for (Method method : klass.getDeclaredMethods()) {
            if (!isValid(method)) continue;
            listeners.add(new Listener(klass, object, method));
        }

        if (klass.getSuperclass() != null) {
            processListeners(listeners, klass.getSuperclass(), object);
        }
    }

    private boolean isValid(Method method) {
        if (!method.isAnnotationPresent(SubscribeEvent.class)) return false;
        if (method.getReturnType() != void.class) return false;
        if (method.getParameterCount() != 1) return false;

        return !method.getParameters()[0].getType().isPrimitive();
    }

    private void insert(List<Listener> listeners, Listener listener) {
        int index;
        for (index = 0; index < listeners.size(); index++) {
            if (listener.getPriority() > listeners.get(index).getPriority()) break;
        }

        listeners.add(index, listener);
    }
}
