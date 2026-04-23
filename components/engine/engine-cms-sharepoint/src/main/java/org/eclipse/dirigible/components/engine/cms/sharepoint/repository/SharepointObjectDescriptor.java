package org.eclipse.dirigible.components.engine.cms.sharepoint.repository;

import java.util.Objects;

class SharepointObjectDescriptor {

    private final String name;

    private final boolean folder;

    private final boolean file;

    SharepointObjectDescriptor(boolean folder, String name) {
        this.folder = folder;
        this.file = !folder;
        this.name = name;
    }

    public boolean isFolder() {
        return folder;
    }

    public boolean isFile() {
        return file;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "SharepointObjectDescriptor{" + "name='" + name + '\'' + ", folder=" + folder + ", file=" + file + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        SharepointObjectDescriptor that = (SharepointObjectDescriptor) o;
        return folder == that.folder && file == that.file && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, folder, file);
    }
}
