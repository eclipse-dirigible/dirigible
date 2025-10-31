package org.eclipse.dirigible.components.engine.di.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.dirigible.components.base.artefact.BaseArtefactService;
import org.eclipse.dirigible.components.engine.di.domain.Component;
import org.eclipse.dirigible.components.engine.di.repository.ComponentRepository;
import org.eclipse.dirigible.components.engine.javascript.service.JavascriptService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.RepositoryNotFoundException;
import org.graalvm.polyglot.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/**
 * The Class EntityService.
 */
@Service
@Transactional
public class ComponentService extends BaseArtefactService<Component, Long> {

    /** The javascript service. */
    private final JavascriptService javascriptService;
    /** The repository. */
    private final IRepository contentRepository;

    /**
     * Instantiates a new component service.
     *
     * @param componentRepository the componentRepository
     * @param javascriptService the javascriptService
     * @param contentRepository the contentRepository
     */
    @Autowired
    public ComponentService(ComponentRepository componentRepository, JavascriptService javascriptService, IRepository contentRepository) {
        super(componentRepository);
        this.javascriptService = javascriptService;
        this.contentRepository = contentRepository;
    }

    /**
     * Gets the javascript handler.
     *
     * @return the javascript handler
     */
    protected JavascriptService getJavascriptService() {
        return javascriptService;
    }

    /**
     * Execute java script.
     *
     * @param projectName the project name
     * @param projectFilePath the project file path
     * @return the response
     */
    public Value executeJavaScript(String projectName, String projectFilePath) {
        try {
            Object object = getJavascriptService().handleRequest(projectName, projectFilePath, null, null, false, true);
            if (object instanceof Value) {
                return (Value) object;
            }
            throw new IllegalArgumentException(
                    "Invalid result of the Component file. The reference of the component class must be the last expression in the file.");
        } catch (RepositoryNotFoundException e) {
            String message = e.getMessage() + ". Try to publish the service before execution.";
            throw new RepositoryNotFoundException(message, e);
        }
    }

}
