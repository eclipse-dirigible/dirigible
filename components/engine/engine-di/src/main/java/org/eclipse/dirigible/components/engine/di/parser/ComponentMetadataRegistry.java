package org.eclipse.dirigible.components.engine.di.parser;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Value;

public class ComponentMetadataRegistry {

    private static final Map<String, Value> injectionsByComponentName = new ConcurrentHashMap<>();

    public static void register(String name, Value injectionsMap) {
        injectionsByComponentName.put(name, injectionsMap);
    }

    public static Value getInjections(String name) {
        return injectionsByComponentName.get(name);
    }

    public static void clear() {
        injectionsByComponentName.clear();
    }
}
