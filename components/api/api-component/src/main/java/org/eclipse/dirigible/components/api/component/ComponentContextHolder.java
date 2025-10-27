package org.eclipse.dirigible.components.api.component;

public class ComponentContextHolder {
    private static final ThreadLocal<String> current = new ThreadLocal<>();

    public static void set(String contextId) {
        current.set(contextId);
    }

    public static String get() {
        return current.get() != null ? current.get() : "default";
    }

    public static void clear() {
        current.remove();
    }
}
