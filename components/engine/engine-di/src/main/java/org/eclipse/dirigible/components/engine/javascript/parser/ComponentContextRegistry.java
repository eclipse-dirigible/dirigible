package org.eclipse.dirigible.components.engine.javascript.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ComponentContextRegistry {

    private static final Map<String, ComponentContext> CONTEXTS = new ConcurrentHashMap<>();

    public static ComponentContext getContext(String contextId) {
        return CONTEXTS.computeIfAbsent(contextId, ComponentContext::new);
    }

    public static void removeContext(String contextId) {
        ComponentContext context = CONTEXTS.remove(contextId);
        if (context != null) {
            context.clear();
        }
    }

    public static void clearAll() {
        CONTEXTS.values()
                .forEach(ComponentContext::clear);
        CONTEXTS.clear();
    }
}
