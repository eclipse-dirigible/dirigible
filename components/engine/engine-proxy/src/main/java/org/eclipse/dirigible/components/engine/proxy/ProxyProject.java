package org.eclipse.dirigible.components.engine.proxy;

public class ProxyProject {

    private final String projectName;
    private final String mappedURL;

    public ProxyProject(String projectName, String mappedURL) {
        this.projectName = projectName;
        this.mappedURL = mappedURL;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getMappedURL() {
        return mappedURL;
    }
}
