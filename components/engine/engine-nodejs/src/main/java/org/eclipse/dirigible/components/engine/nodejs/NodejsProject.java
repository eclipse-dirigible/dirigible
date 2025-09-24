package org.eclipse.dirigible.components.engine.nodejs;

public class NodejsProject {

    private final String projectName;
    private final String mappedURL;

    public NodejsProject(String projectName, String mappedURL) {
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
