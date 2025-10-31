package org.eclipse.dirigible.components.engine.di.parser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.graalvm.polyglot.Value;

public class ComponentContext {

    private final String contextId;
    private final Map<String, Value> components = new ConcurrentHashMap<>();
    private final Map<String, Value> metadata = new ConcurrentHashMap<>();

    public ComponentContext(String contextId) {
        this.contextId = contextId;
    }

    public String getContextId() {
        return contextId;
    }

    public void registerComponent(String name, Value instance, Value injections) {
        components.put(name, instance);
        if (injections != null && !injections.isNull()) {
            metadata.put(name, injections);
        }
    }

    public void registerComponentMetadata(String name, Value jsMap) {
        if (jsMap == null || jsMap.isNull())
            return;

        Map<String, String> snapshot = new HashMap<>();
        Value entries = jsMap.getMember("entries")
                             .execute();
        while (true) {
            Value next = entries.getMember("next")
                                .execute();
            if (next.getMember("done")
                    .asBoolean())
                break;
            Value pair = next.getMember("value");
            String key = pair.getArrayElement(0)
                             .asString();
            Value val = pair.getArrayElement(1);
            snapshot.put(key, val.isNull() ? null : val.asString());
        }

        metadata.put(name, Value.asValue(snapshot)); // or wrap manually
    }

    public Value getComponent(String name) {
        return components.get(name);
    }

    public Value getMetadata(String name) {
        return metadata.get(name);
    }

    public void unregisterComponent(String name) {
        components.remove(name);
        metadata.remove(name);
    }

    public void clear() {
        components.clear();
        metadata.clear();
    }
}
