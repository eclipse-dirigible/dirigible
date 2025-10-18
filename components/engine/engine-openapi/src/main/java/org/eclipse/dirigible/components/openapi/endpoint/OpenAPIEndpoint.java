/*
 * Copyright (c) 2025 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.openapi.endpoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.dirigible.components.base.endpoint.BaseEndpoint;
import org.eclipse.dirigible.components.openapi.service.OpenAPIService;
import org.eclipse.dirigible.components.version.domain.Version;
import org.eclipse.dirigible.components.version.service.VersionService;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import jakarta.annotation.security.RolesAllowed;

/**
 * The Class OpenAPIEndpoint.
 */
@RestController
@RequestMapping(BaseEndpoint.PREFIX_ENDPOINT_SECURED + "openapi")
@RolesAllowed({"ADMINISTRATOR", "DEVELOPER"})
public class OpenAPIEndpoint extends BaseEndpoint {

    /**
     * The Constant logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(OpenAPIEndpoint.class);

    /** The Constant CONTACT_URL. */
    private static final String CONTACT_URL = "https://www.dirigible.io";

    /** The Constant CONTACT_NAME. */
    private static final String CONTACT_NAME = "Eclipse Dirigible";

    /** The Constant CONTACT_EMAIL. */
    private static final String CONTACT_EMAIL = "dirigible-dev@eclipse.org";

    /** The Constant DESCRIPTION. */
    private static final String DESCRIPTION = "Services Open API provided by the applications";

    /** The Constant LICENSE_NAME. */
    private static final String LICENSE_NAME = "Eclipse Public License - v 2.0";

    /** The Constant LICENSE_URL. */
    private static final String LICENSE_URL = "https://www.eclipse.org/legal/epl-v20.html";

    /** The Constant TITLE. */
    private static final String TITLE = "Applications Services Open API";

    /** The Constant VERSION. */
    private static final String VERSION = "12.0.0";

    /**
     * The openapi service.
     */
    @Autowired
    private OpenAPIService openAPIService;

    /**
     * The version service.
     */
    @Autowired
    private VersionService versionService;

    /**
     * Version.
     *
     * @return the response entity
     * @throws JsonProcessingException the json processing exception
     */
    @GetMapping
    public ResponseEntity<String> getVersion() throws JsonProcessingException {
        Contact contactOpenApi = new Contact();
        contactOpenApi.setName(CONTACT_NAME);
        contactOpenApi.setEmail(CONTACT_EMAIL);
        contactOpenApi.setUrl(CONTACT_URL);

        License licenseOpenApi = new License();
        licenseOpenApi.setName(LICENSE_NAME);
        licenseOpenApi.setUrl(LICENSE_URL);

        Info infoOpenApi = new Info();
        infoOpenApi.setContact(contactOpenApi);
        infoOpenApi.setDescription(DESCRIPTION);
        infoOpenApi.setLicense(licenseOpenApi);
        infoOpenApi.setTitle(TITLE);

        try {
            Version version = versionService.getVersion();
            infoOpenApi.setVersion(version.getProductVersion());
        } catch (IOException e) {
            if (logger.isErrorEnabled()) {
                logger.error(e.getMessage(), e);
            }
            infoOpenApi.setVersion(VERSION);
        }

        OpenAPI openApi = initializeOpenApi(infoOpenApi);

        for (org.eclipse.dirigible.components.openapi.domain.OpenAPI openAPI : openAPIService.getAll()) {
            // IResource resource = openAPIService.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC +
            // openAPI.getLocation());
            //
            // if (resource.exists()) {
            // String service = new String(resource.getContent());
            // populateOpenApiFromContribution(openApi, service);
            // }
            populateOpenApiFromContribution(openApi, openAPI.getContent());

        }

        String openAPIJson = io.swagger.v3.core.util.Json.mapper()
                                                         .writeValueAsString(openApi);
        return new ResponseEntity<>(openAPIJson, HttpStatus.OK);
    }

    /**
     * Initialize open api.
     *
     * @param info the info
     * @return the open API
     */
    private OpenAPI initializeOpenApi(Info info) {
        OpenAPI openApi = new OpenAPI();
        openApi.info(info);

        Components components = new Components();
        components.setCallbacks(new HashMap<>());
        components.setExamples(new HashMap<>());
        components.setExtensions(new HashMap<>());
        components.setHeaders(new HashMap<>());
        components.setLinks(new HashMap<>());
        components.setParameters(new HashMap<>());
        components.setPathItems(new HashMap<>());
        components.setRequestBodies(new HashMap<>());
        components.setResponses(new HashMap<>());
        components.setSchemas(new HashMap<>());
        components.setSecuritySchemes(new HashMap<>());

        openApi.setComponents(components);
        openApi.setExtensions(new HashMap<>());
        openApi.setPaths(new Paths());
        openApi.setSecurity(new ArrayList<>());
        openApi.setTags(new ArrayList<>());
        openApi.setWebhooks(new HashMap<>());
        openApi.setServers(new ArrayList<>());

        return openApi;
    }

    /**
     * Populate swagger from contribution.
     *
     * @param openApi the open api
     * @param resource the resource
     */
    private void populateOpenApiFromContribution(OpenAPI openApi, String service) {

        SwaggerParseResult contributionOpenApiParseResult = new OpenAPIV3Parser().readContents(service);
        OpenAPI contributionOpenApi = contributionOpenApiParseResult.getOpenAPI();
        if (contributionOpenApi != null) {
            if (contributionOpenApi.getComponents() != null) {
                if (contributionOpenApi.getComponents()
                                       .getCallbacks() != null) {
                    openApi.getComponents()
                           .getCallbacks()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getCallbacks());
                }
                if (contributionOpenApi.getComponents()
                                       .getExamples() != null) {
                    openApi.getComponents()
                           .getExamples()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getExamples());
                }
                if (contributionOpenApi.getComponents()
                                       .getExtensions() != null) {
                    openApi.getComponents()
                           .getExtensions()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getExtensions());
                }
                if (contributionOpenApi.getComponents()
                                       .getHeaders() != null) {
                    openApi.getComponents()
                           .getHeaders()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getHeaders());
                }
                if (contributionOpenApi.getComponents()
                                       .getLinks() != null) {
                    openApi.getComponents()
                           .getLinks()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getLinks());
                }
                if (contributionOpenApi.getComponents()
                                       .getParameters() != null) {
                    openApi.getComponents()
                           .getParameters()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getParameters());
                }
                if (contributionOpenApi.getComponents()
                                       .getPathItems() != null) {
                    openApi.getComponents()
                           .getPathItems()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getPathItems());
                }
                if (contributionOpenApi.getComponents()
                                       .getRequestBodies() != null) {
                    openApi.getComponents()
                           .getRequestBodies()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getRequestBodies());
                }
                if (contributionOpenApi.getComponents()
                                       .getResponses() != null) {
                    openApi.getComponents()
                           .getResponses()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getResponses());
                }
                if (contributionOpenApi.getComponents()
                                       .getSchemas() != null) {
                    openApi.getComponents()
                           .getSchemas()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getSchemas());
                }
                if (contributionOpenApi.getComponents()
                                       .getSecuritySchemes() != null) {
                    openApi.getComponents()
                           .getSecuritySchemes()
                           .putAll(contributionOpenApi.getComponents()
                                                      .getSecuritySchemes());
                }
            }
            if (contributionOpenApi.getPaths() != null) {
                for (Entry<String, PathItem> path : contributionOpenApi.getPaths()
                                                                       .entrySet()) {
                    openApi.getPaths()
                           .addPathItem(path.getKey(), path.getValue());
                }
            }
            if (contributionOpenApi.getSecurity() != null) {
                openApi.getSecurity()
                       .addAll(contributionOpenApi.getSecurity());
            }
            if (contributionOpenApi.getServers() != null) {
                openApi.getServers()
                       .addAll(contributionOpenApi.getServers());
            }
            if (contributionOpenApi.getTags() != null) {
                openApi.getTags()
                       .addAll(contributionOpenApi.getTags());
            }
        }
    }
}
