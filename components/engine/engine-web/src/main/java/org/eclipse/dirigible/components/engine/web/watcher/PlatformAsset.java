package org.eclipse.dirigible.components.engine.web.watcher;

public class PlatformAsset {

    public enum Type {
        CSS, SCRIPT, PRELOAD
    }

    private final Type type;
    private final String path;
    private final String category;
    private final boolean module;
    private final boolean defer;

    public PlatformAsset(Type type, String path, String category, boolean module, boolean defer) {

        this.type = type;
        this.path = path;
        this.category = category;
        this.module = module;
        this.defer = defer;
    }

    public Type getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public String getCategory() {
        return category;
    }

    public boolean isModule() {
        return module;
    }

    public boolean isDefer() {
        return defer;
    }

}
