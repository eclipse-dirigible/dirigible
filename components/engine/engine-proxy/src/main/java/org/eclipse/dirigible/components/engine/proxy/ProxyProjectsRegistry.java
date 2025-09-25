package org.eclipse.dirigible.components.engine.proxy;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class ProxyProjectsRegistry {

    private final Map<String, ProxyProject> registerdProjects;

    ProxyProjectsRegistry() {
        this.registerdProjects = new HashMap<>();
    }

    public Optional<ProxyProject> findByProjectName(String projectName) {
        ProxyProject proxyProject = registerdProjects.get(projectName);
        return Optional.ofNullable(proxyProject);
    }

    public void register(ProxyProject project) {
        registerdProjects.put(project.getProjectName(), project);
    }

    public void unregister(ProxyProject project) {
        registerdProjects.remove(project.getProjectName());
    }
}
