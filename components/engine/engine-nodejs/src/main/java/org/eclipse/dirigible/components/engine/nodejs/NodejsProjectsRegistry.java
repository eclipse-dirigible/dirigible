package org.eclipse.dirigible.components.engine.nodejs;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class NodejsProjectsRegistry {

    private final Map<String, NodejsProject> registerdProjects;

    NodejsProjectsRegistry() {
        this.registerdProjects = new HashMap<>();
    }

    public Optional<NodejsProject> findByProjectName(String projectName) {
        NodejsProject nodejsProject = registerdProjects.get(projectName);
        return Optional.ofNullable(nodejsProject);
    }

    public void register(NodejsProject project) {
        registerdProjects.put(project.getProjectName(), project);
    }

    public void unregister(NodejsProject project) {
        registerdProjects.remove(project.getProjectName());
    }
}
