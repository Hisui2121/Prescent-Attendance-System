package util;

import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {

    private static CopyOnWriteArrayList<Runnable> studentListeners = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<Runnable> classListeners = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<Runnable> enrollmentListeners = new CopyOnWriteArrayList<>();
    private static CopyOnWriteArrayList<Runnable> notificationListeners = new CopyOnWriteArrayList<>();

    public static void addStudentChangeListener(Runnable r) {
        studentListeners.addIfAbsent(r);
    }

    public static void removeStudentChangeListener(Runnable r) {
        studentListeners.remove(r);
    }

    public static void fireStudentChanged() {
        for (Runnable r : studentListeners) {
            try { r.run(); } catch (Exception ex) {  }
        }
    }

    public static void addClassChangeListener(Runnable r) {
        classListeners.addIfAbsent(r);
    }

    public static void removeClassChangeListener(Runnable r) {
        classListeners.remove(r);
    }

    public static void fireClassChanged() {
        for (Runnable r : classListeners) {
            try { r.run(); } catch (Exception ex) { }
        }
    }

    // Enrollment change listeners
    public static void addEnrollmentChangeListener(Runnable r) {
        enrollmentListeners.addIfAbsent(r);
    }

    public static void removeEnrollmentChangeListener(Runnable r) {
        enrollmentListeners.remove(r);
    }

    public static void fireEnrollmentChanged() {
        for (Runnable r : enrollmentListeners) {
            try { r.run(); } catch (Exception ex) {  }
        }
    }

    // Notification change listeners
    public static void addNotificationChangeListener(Runnable r) {
        notificationListeners.addIfAbsent(r);
    }

    public static void removeNotificationChangeListener(Runnable r) {
        notificationListeners.remove(r);
    }

    public static void fireNotificationChanged() {
        for (Runnable r : notificationListeners) {
            try { r.run(); } catch (Exception ex) { }
        }
    }

    // Session change listeners (fired when attendance sessions are created/deleted)
    private static CopyOnWriteArrayList<Runnable> sessionListeners = new CopyOnWriteArrayList<>();

    public static void addSessionChangeListener(Runnable r) {
        sessionListeners.addIfAbsent(r);
    }

    public static void removeSessionChangeListener(Runnable r) {
        sessionListeners.remove(r);
    }

    public static void fireSessionChanged() {
        for (Runnable r : sessionListeners) {
            try { r.run(); } catch (Exception ex) { }
        }
    }
}