package emanondev.itemtag.utility;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/** Runtime bridge for Folia ownership schedulers while retaining Paper's legacy scheduler. */
public final class TaskScheduler {
    private static final Method ENTITY_SCHEDULER = find(Entity.class, "getScheduler");

    private TaskScheduler() { }

    public static void runAtEntity(Plugin plugin, Entity entity, long delayTicks, Runnable task) {
        Object scheduler = invoke(ENTITY_SCHEDULER, entity);
        if (scheduler == null) {
            Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
            return;
        }
        Consumer<Object> callback = ignored -> task.run();
        if (delayTicks <= 1)
            invoke(required(scheduler.getClass(), "run", Plugin.class, Consumer.class, Runnable.class),
                    scheduler, plugin, callback, null);
        else
            invoke(required(scheduler.getClass(), "runDelayed", Plugin.class, Consumer.class, Runnable.class, long.class),
                    scheduler, plugin, callback, null, delayTicks);
    }

    public static Object repeatAtEntity(Plugin plugin, Entity entity, long initialDelay, long period, Runnable task) {
        Object scheduler = invoke(ENTITY_SCHEDULER, entity);
        if (scheduler == null)
            return Bukkit.getScheduler().runTaskTimer(plugin, task, initialDelay, period);
        Consumer<Object> callback = ignored -> task.run();
        return invoke(required(scheduler.getClass(), "runAtFixedRate", Plugin.class, Consumer.class,
                Runnable.class, long.class, long.class), scheduler, plugin, callback, null, initialDelay, period);
    }

    public static void cancel(Object task) {
        if (task != null) invoke(find(task.getClass(), "cancel"), task);
    }

    private static Method required(Class<?> type, String name, Class<?>... args) {
        Method method = find(type, name, args);
        if (method == null) {
            throw new IllegalStateException("Folia entity scheduler lacks required API: " + name);
        }
        return method;
    }

    private static Object invoke(Method method, Object receiver, Object... args) {
        if (method == null) return null;
        try { return method.invoke(receiver, args); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException("Unable to invoke scheduler API " + method, e); }
    }

    private static Method find(Class<?> type, String name, Class<?>... args) {
        try { return type.getMethod(name, args); }
        catch (ReflectiveOperationException ignored) { return null; }
    }
}
