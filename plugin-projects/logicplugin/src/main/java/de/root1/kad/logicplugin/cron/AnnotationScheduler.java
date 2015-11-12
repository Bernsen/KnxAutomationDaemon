package de.root1.kad.logicplugin.cron;

import it.sauronsoftware.cron4j.Scheduler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cron scheduler implementation. Use AnnotationScheduler::schedule method to
 * enable scheduling for all methods annotated with Scheduled annotation.
 */
public class AnnotationScheduler {

    private Logger log = LoggerFactory.getLogger(getClass());
    private final Scheduler s;

    public AnnotationScheduler() {
        s = new Scheduler();

        s.start();
    }

    public void schedule(final Object obj) {
        for (Class<?> cls = obj.getClass(); cls != Object.class; cls = cls.getSuperclass()) {
            try {
                for (final Method m : cls.getDeclaredMethods()) {
                    final Scheduled annotation = m.getAnnotation(Scheduled.class);
                    if (annotation != null) {
                        m.setAccessible(true);

                        s.schedule(annotation.cron(), new Runnable() {

                            @Override
                            public void run() {
                                try {
                                    Thread.currentThread().setName("cron");
                                    m.invoke(obj);
                                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
                                    log.warn("cannot execute con method: " + m.toGenericString() + " at [" + annotation.cron(), ex);
                                }
                            }
                        });

                    }
                }
            } catch (SecurityException ex) {
                log.error("SecurityException while schduling cron method", ex);
            }
        }
    }

}
