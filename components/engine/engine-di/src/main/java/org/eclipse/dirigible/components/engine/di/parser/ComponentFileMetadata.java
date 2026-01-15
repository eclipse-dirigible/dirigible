package org.eclipse.dirigible.components.engine.di.parser;

public class ComponentFileMetadata {

    private String name;
    private String location;
    private String projectName;
    private String filePath;
    private String contextId;



    public ComponentFileMetadata(String name, String location, String projectName, String filePath, String contextId) {
        super();
        this.name = name;
        this.location = location;
        this.projectName = projectName;
        this.filePath = filePath;
        this.contextId = contextId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the projectName
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * @param projectName the projectName to set
     */
    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    /**
     * @return the filePath
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * @param filePath the filePath to set
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * @return the contextId
     */
    public String getContextId() {
        return contextId;
    }

    /**
     * @param contextId the contextId to set
     */
    public void setContextId(String contextId) {
        this.contextId = contextId;
    }



}
