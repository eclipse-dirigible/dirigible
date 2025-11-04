package org.eclipse.dirigible.components.listeners.parser;

/**
 * Simple metadata holder for @Listener-decorated classes.
 */
public class ListenerMetadata {

    private String className;
    private String name;
    private String kind;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return "ListenerMetadata{" + "className='" + className + '\'' + ", name='" + name + '\'' + ", kind='" + kind + '\'' + '}';
    }
}
