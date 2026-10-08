/*
 * Copyright (c) 2010-2026 Eclipse Dirigible contributors
 *
 * All rights reserved. This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-FileCopyrightText: Eclipse Dirigible contributors SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.dirigible.components.engine.document;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FilenameUtils;
import org.eclipse.dirigible.commons.config.Configuration;
import org.eclipse.dirigible.components.base.artefact.ArtefactLifecycle;
import org.eclipse.dirigible.components.base.artefact.ArtefactPhase;
import org.eclipse.dirigible.components.base.artefact.ArtefactService;
import org.eclipse.dirigible.components.base.artefact.topology.TopologyWrapper;
import org.eclipse.dirigible.components.base.synchronizer.MultitenantBaseSynchronizer;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizerCallback;
import org.eclipse.dirigible.components.base.synchronizer.SynchronizersOrder;
import org.eclipse.dirigible.components.engine.document.domain.PrintTemplateSeed;
import org.eclipse.dirigible.components.engine.document.service.PrintTemplateSeedService;
import org.eclipse.dirigible.repository.api.IRepository;
import org.eclipse.dirigible.repository.api.IRepositoryStructure;
import org.eclipse.dirigible.repository.api.IResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Seeds the print templates a project ships at
 * {@code doc/Templates/<Entity>/Print/<lang>/<name>.print} into every tenant's CMS as immutable
 * versions, {@code Templates/<Entity>/Print/<lang>/<name>@<version>.print}, beside the versions
 * shipped before - see {@link PrintTemplateCatalog#seed} for what a tenant's catalogue receives.
 * The generic {@link CmsSeedSynchronizer} leaves these files alone, so the two never both seed one
 * file.
 *
 * <p>
 * The version is the module's release version - the {@code version} field of the project's
 * {@code project.json} - when it declares a valid one, else the first 8 hex digits of the content's
 * SHA-256. Deleting the file removes the artefact row only; the versions it shipped stay in every
 * tenant's catalogue.
 */
@Component
@Order(SynchronizersOrder.PRINT_TEMPLATE)
class PrintTemplateSynchronizer extends MultitenantBaseSynchronizer<PrintTemplateSeed, Long> {

    private static final Logger logger = LoggerFactory.getLogger(PrintTemplateSynchronizer.class);

    /** The project-relative folder whose contents are mirrored into the CMS. */
    private static final String DOC_SEGMENT = "/doc/";

    private static final String TEMPLATES_SEGMENT = "Templates";
    private static final String PRINT_SEGMENT = "Print";
    private static final String PROJECT_FILE = "project.json";
    private static final String VERSION_PROPERTY = "version";
    private static final int HASH_VERSION_LENGTH = 8;

    private final PrintTemplateSeedService printTemplateSeedService;
    private final PrintTemplateCatalog catalog;
    private final IRepository repository;

    private SynchronizerCallback callback;

    PrintTemplateSynchronizer(PrintTemplateSeedService printTemplateSeedService, PrintTemplateCatalog catalog, IRepository repository) {
        this.printTemplateSeedService = printTemplateSeedService;
        this.catalog = catalog;
        this.repository = repository;
    }

    /**
     * A shipped print template's place:
     * {@code <project>/doc/Templates/<Entity>/Print/<lang>/<file>.print}.
     *
     * @param project the project the file ships in
     * @param entity the document type
     * @param language the language code
     * @param name the template name - the file name without the extension (and without any
     *        {@code @version} the file name carries)
     */
    record ShippedLocation(String project, String entity, String language, String name) {

        /**
         * Parses a registry-relative location or file path.
         *
         * @param location the location
         * @return the shipped location, empty when the file is not a shipped print template
         */
        static Optional<ShippedLocation> of(String location) {
            String normalized = location.replace('\\', '/');
            int doc = normalized.indexOf(DOC_SEGMENT);
            if (doc < 0) {
                return Optional.empty();
            }
            String[] segments = normalized.substring(doc + DOC_SEGMENT.length())
                                          .split("/");
            if (segments.length != 5 || !TEMPLATES_SEGMENT.equals(segments[0]) || !PRINT_SEGMENT.equals(segments[2])
                    || !PrintTemplateCatalog.isSegment(segments[1]) || !PrintTemplateCatalog.isSegment(segments[3])) {
                return Optional.empty();
            }
            Optional<PrintTemplateName> file = PrintTemplateName.fromFileName(segments[4]);
            if (file.isEmpty()) {
                return Optional.empty();
            }
            String[] head = normalized.substring(0, doc)
                                      .split("/");
            String project = head.length == 0 ? "" : head[head.length - 1];
            return Optional.of(new ShippedLocation(project, segments[1], segments[3], file.get()
                                                                                          .name()));
        }
    }

    /**
     * Whether a location is a shipped print template - also how {@link CmsSeedSynchronizer} knows to
     * leave it alone.
     *
     * @param location the registry-relative location or file path
     * @return true for {@code <project>/doc/Templates/<Entity>/Print/<lang>/<file>.print}
     */
    static boolean isShippedTemplate(String location) {
        return ShippedLocation.of(location)
                              .isPresent();
    }

    @Override
    public boolean isAccepted(Path file, BasicFileAttributes attrs) {
        return attrs.isRegularFile() && isShippedTemplate(file.toString());
    }

    @Override
    public boolean isAccepted(String type) {
        return PrintTemplateSeed.ARTEFACT_TYPE.equals(type);
    }

    @Override
    protected List<PrintTemplateSeed> parseImpl(String location, byte[] content) throws ParseException {
        ShippedLocation shipped = ShippedLocation.of(location)
                                                 .orElseThrow(() -> new ParseException("Not a shipped print template: " + location, 0));
        PrintTemplateSeed seed = new PrintTemplateSeed();
        Configuration.configureObject(seed);
        seed.setLocation(location);
        seed.setName(FilenameUtils.getName(location));
        seed.setType(PrintTemplateSeed.ARTEFACT_TYPE);
        seed.setEntityName(shipped.entity());
        seed.setLanguage(shipped.language());
        seed.setTemplateName(shipped.name());
        seed.setVersion(releaseVersion(shipped.project()).orElseGet(() -> contentVersion(content)));
        seed.setContent(content);
        seed.updateKey();
        try {
            PrintTemplateSeed maybe = getService().findByKey(seed.getKey());
            if (maybe != null) {
                seed.setId(maybe.getId());
            }
            seed = getService().save(seed);
        } catch (Exception e) {
            logger.error("Failed to save shipped print template [{}]", seed, e);
            throw new ParseException(e.getMessage(), 0);
        }
        return List.of(seed);
    }

    /** The {@code version} of the project's {@code project.json}, when it declares a valid one. */
    private Optional<String> releaseVersion(String project) {
        if (project.isEmpty()) {
            return Optional.empty();
        }
        IResource descriptor = repository.getResource(IRepositoryStructure.PATH_REGISTRY_PUBLIC + "/" + project + "/" + PROJECT_FILE);
        if (!descriptor.exists()) {
            return Optional.empty();
        }
        try {
            JsonElement json = JsonParser.parseString(new String(descriptor.getContent(), StandardCharsets.UTF_8));
            if (json.isJsonObject()) {
                JsonObject object = json.getAsJsonObject();
                if (object.has(VERSION_PROPERTY) && object.get(VERSION_PROPERTY)
                                                          .isJsonPrimitive()) {
                    String version = object.get(VERSION_PROPERTY)
                                           .getAsString()
                                           .trim();
                    if (PrintTemplateName.isValidVersion(version)) {
                        return Optional.of(version);
                    }
                    logger.warn("The version [{}] of project [{}] cannot name a print template version - using the content hash", version,
                            project);
                }
            }
        } catch (RuntimeException e) {
            logger.warn("Cannot read the version of project [{}] - using the content hash for its print templates", project, e);
        }
        return Optional.empty();
    }

    /** The first 8 hex digits of the content's SHA-256. */
    static String contentVersion(byte[] content) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                                         .digest(content);
            return HexFormat.of()
                            .formatHex(digest)
                            .substring(0, HASH_VERSION_LENGTH);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    @Override
    public ArtefactService<PrintTemplateSeed, Long> getService() {
        return printTemplateSeedService;
    }

    @Override
    public List<PrintTemplateSeed> retrieve(String location) {
        return getService().findByLocation(location);
    }

    @Override
    public void setStatus(PrintTemplateSeed artefact, ArtefactLifecycle lifecycle, String error) {
        artefact.setLifecycle(lifecycle);
        artefact.setError(error);
        getService().save(artefact);
    }

    @Override
    protected boolean completeImpl(TopologyWrapper<PrintTemplateSeed> wrapper, ArtefactPhase flow) {
        PrintTemplateSeed seed = wrapper.getArtefact();
        ArtefactLifecycle lifecycle = seed.getLifecycle();

        switch (flow) {
            case CREATE:
                if (ArtefactLifecycle.NEW.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case UPDATE:
                if (ArtefactLifecycle.MODIFIED.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.UPDATED);
                }
                break;
            case DELETE:
                if (ArtefactLifecycle.CREATED.equals(lifecycle) || ArtefactLifecycle.UPDATED.equals(lifecycle)
                        || ArtefactLifecycle.FAILED.equals(lifecycle)) {
                    // remove the database row only - the shipped versions stay in every tenant's catalogue
                    try {
                        getService().delete(seed);
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED);
                    } catch (Exception e) {
                        callback.addError(e.getMessage());
                        callback.registerState(this, wrapper, ArtefactLifecycle.DELETED, e);
                    }
                }
                break;
            case START:
                // A seed that failed (the CMS or the tenant configuration not reachable yet) is retried.
                if (ArtefactLifecycle.FAILED.equals(lifecycle)) {
                    return seed(wrapper, ArtefactLifecycle.CREATED);
                }
                break;
            case STOP:
                break;
        }
        return true;
    }

    /**
     * Seeds the template into the current tenant's catalogue and registers the given lifecycle state.
     */
    private boolean seed(TopologyWrapper<PrintTemplateSeed> wrapper, ArtefactLifecycle lifecycle) {
        PrintTemplateSeed seed = wrapper.getArtefact();
        try {
            catalog.seed(seed.getEntityName(), seed.getLanguage(), seed.getTemplateName(), seed.getVersion(), seed.getContent());
            callback.registerState(this, wrapper, lifecycle);
            return true;
        } catch (Exception e) {
            logger.error("Failed to seed print template [{}@{}] of [{}/{}]", seed.getTemplateName(), seed.getVersion(),
                    seed.getEntityName(), seed.getLanguage(), e);
            callback.addError(e.getMessage());
            callback.registerState(this, wrapper, ArtefactLifecycle.FAILED, e);
            return false;
        }
    }

    @Override
    public void cleanupImpl(PrintTemplateSeed seed) {
        // never delete from the CMS - shipped versions stay in every tenant's catalogue
        try {
            getService().delete(seed);
        } catch (Exception e) {
            callback.addError(e.getMessage());
            callback.registerState(this, seed, ArtefactLifecycle.DELETED, e);
        }
    }

    @Override
    public void setCallback(SynchronizerCallback callback) {
        this.callback = callback;
    }

    @Override
    public String getFileExtension() {
        return PrintTemplateName.EXTENSION;
    }

    @Override
    public String getArtefactType() {
        return PrintTemplateSeed.ARTEFACT_TYPE;
    }
}
