package org.eclipse.dirigible.flowable.bpmn.docs.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class ElementDoc {
    private final Map<String, Object> metadata = new LinkedHashMap<>();
    private String id;
    private String type;
    private String name;
    private Object impl;

    public ElementDoc(String id, String type, String name, Object impl) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.impl = impl;
    }

    public Object getImpl() {
        return impl;
    }

    public void setImpl(Object impl) {
        this.impl = impl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }
}
