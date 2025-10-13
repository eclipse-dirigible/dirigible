package org.eclipse.dirigible.components.data.store.model;

import java.util.ArrayList;
import java.util.List;

public class EntityMetadata {

    private String entityName;

    private String tableName;

    private String className;

    private List<EntityFieldMetadata> fields = new ArrayList<>();

    // Getters and Setters (simplified)
    public String getEntityName() {
        return entityName;
    }

    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<EntityFieldMetadata> getFields() {
        return new ArrayList<>(fields);
    }

    public void setFields(List<EntityFieldMetadata> fields) {
        this.fields = (fields == null) ? new ArrayList<>() : new ArrayList<>(fields);
    }

}
